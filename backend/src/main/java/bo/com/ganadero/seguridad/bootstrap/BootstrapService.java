package bo.com.ganadero.seguridad.bootstrap;

import bo.com.ganadero.seguridad.infrastructure.SupabaseAuthAdminClient;
import bo.com.ganadero.shared.config.AppProperties;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import jakarta.validation.Validator;

@Service
public class BootstrapService {
    private final AppProperties properties; private final SupabaseAuthAdminClient auth;
    private final JdbcClient jdbc; private final TransactionTemplate transactions; private final Validator validator;
    public BootstrapService(AppProperties properties,SupabaseAuthAdminClient auth,JdbcClient jdbc,
            TransactionTemplate transactions,Validator validator){this.properties=properties;this.auth=auth;this.jdbc=jdbc;this.transactions=transactions;this.validator=validator;}

    public BootstrapResponse execute(String suppliedToken,String key,BootstrapRequest request,String correlationId){
        validateAccess(suppliedToken,key);
        if(request==null||!validator.validate(request).isEmpty())throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        String hash=payloadHash(request);
        Optional<BootstrapResponse> previous=previous(key,hash); if(previous.isPresent())return previous.get();
        if(jdbc.sql("select count(*) from seguridad.bootstrap_ejecuciones where estado='COMPLETADO'").query(Long.class).single()>0)
            throw new BusinessException(ErrorCode.BOOTSTRAP_ALREADY_COMPLETED);
        SupabaseAuthAdminClient.AdminUser user=auth.invite(request.propietario().email(),properties.frontendUrl()+"/auth/aceptar-invitacion");
        try{return Objects.requireNonNull(transactions.execute(status->createLocal(key,hash,request,user.id(),correlationId)));}
        catch(RuntimeException exception){auth.deleteIfCreated(user);throw exception;}
    }

    private BootstrapResponse createLocal(String key,String hash,BootstrapRequest request,UUID userId,String correlationId){
        UUID executionId=UUID.randomUUID(),empresaId=UUID.randomUUID(),miembroId=UUID.randomUUID(),propiedadId=UUID.randomUUID();
        jdbc.sql("insert into seguridad.bootstrap_ejecuciones(id,idempotency_key,payload_hash,estado) values(:id,:key,:hash,'PROCESANDO')")
                .param("id",executionId).param("key",key).param("hash",hash).update();
        var e=request.empresa();var owner=request.propietario();var property=request.propiedadInicial();
        jdbc.sql("""
            insert into core.empresas(id,codigo,razon_social,nombre_comercial,nit,telefono,email,direccion,zona_horaria,moneda)
            values(:id,:codigo,:razon,:nombre,:nit,:telefono,:email,:direccion,:zona,:moneda)
            """)
            .param("id",empresaId).param("codigo",e.codigo()).param("razon",e.razonSocial()).param("nombre",e.nombreComercial())
            .param("nit",e.nit()).param("telefono",e.telefono()).param("email",e.email()).param("direccion",e.direccion())
            .param("zona",defaultIfBlank(e.zonaHoraria(),"America/La_Paz")).param("moneda",defaultIfBlank(e.moneda(),"BOB")).update();
        jdbc.sql("insert into core.configuraciones_empresa(empresa_id,moneda) values(:id,:moneda)")
            .param("id",empresaId).param("moneda",defaultIfBlank(e.moneda(),"BOB")).update();
        jdbc.sql("""
            insert into seguridad.perfiles_usuario(id,email,nombres,apellidos,telefono,activo)
            values(:id,:email,:nombres,:apellidos,:telefono,true)
            on conflict(id) do update set email=excluded.email,nombres=excluded.nombres,apellidos=excluded.apellidos,telefono=excluded.telefono
            """)
            .param("id",userId).param("email",owner.email().toLowerCase(Locale.ROOT)).param("nombres",owner.nombres())
            .param("apellidos",owner.apellidos()).param("telefono",owner.telefono()).update();
        jdbc.sql("""
            insert into seguridad.miembros_empresa(id,empresa_id,usuario_id,cargo,estado,fecha_ingreso,acceso_todas_propiedades)
            values(:id,:empresa,:usuario,:cargo,'ACTIVO',current_date,true)
            """).param("id",miembroId).param("empresa",empresaId)
            .param("usuario",userId).param("cargo",defaultIfBlank(owner.cargo(),"PROPIETARIO")).update();
        UUID ownerRole=jdbc.sql("select id from seguridad.roles where codigo='PROPIETARIO' and empresa_id is null").query(UUID.class).single();
        jdbc.sql("insert into seguridad.usuario_roles(miembro_empresa_id,rol_id) values(:miembro,:rol)").param("miembro",miembroId).param("rol",ownerRole).update();
        jdbc.sql("""
            insert into core.propiedades(id,empresa_id,codigo,nombre,descripcion,departamento,municipio,localidad,direccion_referencia,superficie_ha,created_by,updated_by)
            values(:id,:empresa,:codigo,:nombre,:descripcion,:departamento,:municipio,:localidad,:direccion,:superficie,:usuario,:usuario)
            """)
            .param("id",propiedadId).param("empresa",empresaId).param("codigo",property.codigo()).param("nombre",property.nombre())
            .param("descripcion",property.descripcion()).param("departamento",property.departamento()).param("municipio",property.municipio())
            .param("localidad",property.localidad()).param("direccion",property.direccionReferencia()).param("superficie",property.superficieHa())
            .param("usuario",userId).update();
        jdbc.sql("""
            insert into auditoria.registros(id,empresa_id,usuario_id,accion,modulo,entidad,entidad_id,correlation_id,resultado,datos)
            values(:id,:empresa,:usuario,'BOOTSTRAP_INICIAL','SEGURIDAD','EMPRESA',:empresa,:correlation,'COMPLETADO','{}'::jsonb)
            """)
            .param("id",UUID.randomUUID()).param("empresa",empresaId).param("usuario",userId).param("correlation",correlationId).update();
        jdbc.sql("""
            update seguridad.bootstrap_ejecuciones set estado='COMPLETADO',empresa_id=:empresa,usuario_id=:usuario,
            miembro_id=:miembro,propiedad_id=:propiedad,completed_at=now() where id=:id
            """).param("empresa",empresaId)
            .param("usuario",userId).param("miembro",miembroId).param("propiedad",propiedadId).param("id",executionId).update();
        return new BootstrapResponse(empresaId,userId,miembroId,propiedadId,"COMPLETADO");
    }

    private Optional<BootstrapResponse> previous(String key,String hash){return jdbc.sql("""
        select payload_hash,empresa_id,usuario_id,miembro_id,propiedad_id,estado
        from seguridad.bootstrap_ejecuciones where idempotency_key=:key
        """).param("key",key).query((rs,row)->{
            if(!hash.equals(rs.getString("payload_hash"))||!"COMPLETADO".equals(rs.getString("estado")))throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
            return new BootstrapResponse(rs.getObject("empresa_id",UUID.class),rs.getObject("usuario_id",UUID.class),rs.getObject("miembro_id",UUID.class),rs.getObject("propiedad_id",UUID.class),"COMPLETADO");}).optional();}
    private void validateAccess(String supplied,String key){
        if(!properties.bootstrap().enabled())throw new BusinessException(ErrorCode.BOOTSTRAP_DISABLED);
        if(key==null||key.isBlank()||key.length()>200)throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        byte[] expected=properties.bootstrap().token()==null?new byte[0]:properties.bootstrap().token().getBytes(StandardCharsets.UTF_8);
        byte[] actual=supplied==null?new byte[0]:supplied.getBytes(StandardCharsets.UTF_8);
        if(expected.length==0||!MessageDigest.isEqual(expected,actual))throw new BusinessException(ErrorCode.BOOTSTRAP_TOKEN_INVALID);
    }
    private String payloadHash(BootstrapRequest request){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(request.toString().getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException exception){throw new BusinessException(ErrorCode.INTERNAL_ERROR);}}
    private String defaultIfBlank(String value,String fallback){return value==null||value.isBlank()?fallback:value;}
}
