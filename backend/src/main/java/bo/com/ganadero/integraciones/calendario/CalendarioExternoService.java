package bo.com.ganadero.integraciones.calendario;

import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class CalendarioExternoService {
    private static final String PROVEEDOR = "GOOGLE_CALENDAR";
    private static final String PERMISO = "SANIDAD_CONFIGURAR";
    private final JdbcClient jdbc;
    private final UserContext context;

    public CalendarioExternoService(JdbcClient jdbc, UserContext context) {
        this.jdbc = jdbc;
        this.context = context;
    }

    @Transactional(readOnly = true)
    public ConfiguracionCalendarioExterno consultar() {
        CurrentUser u = context.requirePermission(PERMISO);
        return buscarConfiguracion(u.empresaId());
    }

    @Transactional
    public ConfiguracionCalendarioExterno guardar(GuardarConfiguracion command) {
        CurrentUser u = context.requirePermission(PERMISO);
        if (command.calendarioNombre() == null || command.calendarioNombre().isBlank())
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        try { ZoneId.of(command.zonaHoraria()); }
        catch (Exception ex) { throw new BusinessException(ErrorCode.VALIDATION_ERROR); }
        ConfiguracionCalendarioExterno actual = buscarConfiguracion(u.empresaId());
        EstadoConexionCalendario estado = command.sincronizacionAutomatica()
                ? (actual.calendarioExternoId() == null ? EstadoConexionCalendario.PENDIENTE_AUTORIZACION : actual.estado())
                : EstadoConexionCalendario.DESHABILITADO;
        jdbc.sql("""
                insert into configuracion_calendario_externo
                    (id,empresa_id,proveedor,cuenta_email,calendario_externo_id,calendario_nombre,zona_horaria,
                     sincronizacion_automatica,estado,version)
                values(:id,:empresa,:proveedor,:email,:calendario,:nombre,:zona,:auto,:estado,0)
                on conflict(empresa_id,proveedor) do update set
                    cuenta_email=excluded.cuenta_email, calendario_nombre=excluded.calendario_nombre,
                    zona_horaria=excluded.zona_horaria, sincronizacion_automatica=excluded.sincronizacion_automatica,
                    estado=excluded.estado, updated_at=current_timestamp, version=version+1
                """).param("id", actual.id().toString()).param("empresa", u.empresaId().toString())
                .param("proveedor", PROVEEDOR).param("email", limpio(command.cuentaEmail()))
                .param("calendario", actual.calendarioExternoId()).param("nombre", command.calendarioNombre().trim())
                .param("zona", command.zonaHoraria()).param("auto", command.sincronizacionAutomatica())
                .param("estado", estado.name()).update();
        if (command.sincronizacionAutomatica()) encolarOcurrenciasPendientes(u.empresaId());
        return buscarConfiguracion(u.empresaId());
    }

    @Transactional
    public List<TrabajoSincronizacionCalendario> reclamar(int limite, String dispositivoId) {
        return reclamar(limite, dispositivoId, false);
    }

    @Transactional
    public List<TrabajoSincronizacionCalendario> reclamar(int limite, String dispositivoId, boolean manual) {
        CurrentUser u = context.requirePermission(PERMISO);
        if (limite < 1 || limite > 50 || dispositivoId == null || dispositivoId.isBlank())
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        ConfiguracionCalendarioExterno config = buscarConfiguracion(u.empresaId());
        if (config.calendarioExternoId() == null) return List.of();
        if (!manual && (!config.sincronizacionAutomatica() || config.estado()!=EstadoConexionCalendario.CONECTADO)) return List.of();
        encolarDesenlaces(u.empresaId());
        liberarBloqueosExpirados(u.empresaId());
        List<String> ids = jdbc.sql("""
                select id from cola_sincronizacion_calendario
                where empresa_id=:empresa and estado in ('PENDIENTE','REINTENTO') and proximo_intento<=:ahora
                order by created_at limit :limite
                """).param("empresa", u.empresaId().toString()).param("ahora", Instant.now().toString())
                .param("limite", limite).query(String.class).list();
        Instant hasta = Instant.now().plus(Duration.ofMinutes(5));
        for (String id : ids) jdbc.sql("""
                update cola_sincronizacion_calendario set estado='PROCESANDO', bloqueado_por=:dispositivo,
                    bloqueado_hasta=:hasta, intentos=intentos+1, updated_at=current_timestamp, version=version+1
                where id=:id and estado in ('PENDIENTE','REINTENTO')
                """).param("dispositivo", dispositivoId.trim()).param("hasta", hasta.toString()).param("id", id).update();
        return trabajos(u.empresaId(), ids);
    }

    @Transactional
    public ResumenCola prepararSincronizacionManual() {
        CurrentUser u = context.requirePermission(PERMISO);
        encolarOcurrenciasPendientes(u.empresaId());
        encolarDesenlaces(u.empresaId());
        return resumenColaInterno(u.empresaId());
    }

    @Transactional
    public ConfiguracionCalendarioExterno confirmarConexion(ConexionConfirmada command) {
        CurrentUser u = context.requirePermission(PERMISO);
        if (command.calendarioExternoId()==null || command.calendarioExternoId().isBlank())
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        jdbc.sql("""
                insert into configuracion_calendario_externo
                    (id,empresa_id,proveedor,cuenta_email,calendario_externo_id,sincronizacion_automatica,estado)
                values(:id,:empresa,:proveedor,:email,:calendario,1,'CONECTADO')
                on conflict(empresa_id,proveedor) do update set cuenta_email=excluded.cuenta_email,
                    calendario_externo_id=excluded.calendario_externo_id,estado='CONECTADO',ultimo_error=null,
                    updated_at=current_timestamp,version=version+1
                """).param("id",UUID.randomUUID().toString()).param("email",limpio(command.cuentaEmail()))
                .param("calendario",command.calendarioExternoId().trim())
                .param("empresa",u.empresaId().toString()).param("proveedor",PROVEEDOR).update();
        encolarOcurrenciasPendientes(u.empresaId());
        return buscarConfiguracion(u.empresaId());
    }

    @Transactional
    public ConfiguracionCalendarioExterno registrarAutorizacion(AutorizacionConfirmada command) {
        CurrentUser u = context.requirePermission(PERMISO);
        if (command.cuentaEmail()==null || command.cuentaEmail().isBlank())
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        ConfiguracionCalendarioExterno actual = buscarConfiguracion(u.empresaId());
        jdbc.sql("""
                insert into configuracion_calendario_externo
                    (id,empresa_id,proveedor,cuenta_email,calendario_nombre,zona_horaria,
                     sincronizacion_automatica,estado)
                values(:id,:empresa,:proveedor,:email,:nombre,:zona,1,'PENDIENTE_AUTORIZACION')
                on conflict(empresa_id,proveedor) do update set cuenta_email=excluded.cuenta_email,
                    calendario_externo_id=null,estado='PENDIENTE_AUTORIZACION',ultimo_error=null,
                    updated_at=current_timestamp,version=version+1
                """).param("id",actual.id().toString()).param("empresa",u.empresaId().toString())
                .param("proveedor",PROVEEDOR).param("email",command.cuentaEmail().trim())
                .param("nombre",actual.calendarioNombre()).param("zona",actual.zonaHoraria()).update();
        return buscarConfiguracion(u.empresaId());
    }

    @Transactional
    public ConfiguracionCalendarioExterno revocarAutorizacion() {
        CurrentUser u = context.requirePermission(PERMISO);
        ConfiguracionCalendarioExterno actual = buscarConfiguracion(u.empresaId());
        jdbc.sql("""
                insert into configuracion_calendario_externo
                    (id,empresa_id,proveedor,calendario_nombre,zona_horaria,sincronizacion_automatica,estado)
                values(:id,:empresa,:proveedor,:nombre,:zona,0,'DESCONECTADO')
                on conflict(empresa_id,proveedor) do update set cuenta_email=null,calendario_externo_id=null,
                    sincronizacion_automatica=0,estado='DESCONECTADO',ultimo_error=null,
                    updated_at=current_timestamp,version=version+1
                """).param("id",actual.id().toString()).param("empresa",u.empresaId().toString())
                .param("proveedor",PROVEEDOR).param("nombre",actual.calendarioNombre())
                .param("zona",actual.zonaHoraria()).update();
        jdbc.sql("""
                update cola_sincronizacion_calendario set estado='CANCELADO',bloqueado_por=null,bloqueado_hasta=null,
                    updated_at=current_timestamp,version=version+1
                where empresa_id=:empresa and estado in ('PENDIENTE','REINTENTO','PROCESANDO')
                """).param("empresa",u.empresaId().toString()).update();
        return buscarConfiguracion(u.empresaId());
    }

    @Transactional(readOnly = true)
    public ResumenCola resumenCola() {
        CurrentUser u = context.requirePermission(PERMISO);
        List<ConteoEstado> conteos = jdbc.sql("""
                select estado,count(*) cantidad from cola_sincronizacion_calendario
                where empresa_id=:empresa group by estado
                """).param("empresa",u.empresaId().toString())
                .query((r,n)->new ConteoEstado(EstadoColaCalendario.valueOf(r.getString("estado")),r.getLong("cantidad"))).list();
        return new ResumenCola(buscarConfiguracion(u.empresaId()),conteos);
    }

    @Transactional
    public ResumenCola reintentarErrores() {
        CurrentUser u = context.requirePermission(PERMISO);
        jdbc.sql("""
                update cola_sincronizacion_calendario set estado='REINTENTO',intentos=0,
                    proximo_intento=:ahora,bloqueado_por=null,bloqueado_hasta=null,
                    updated_at=current_timestamp,version=version+1
                where empresa_id=:empresa and estado='ERROR_DEFINITIVO'
                """).param("ahora",Instant.now().toString()).param("empresa",u.empresaId().toString()).update();
        jdbc.sql("""
                update configuracion_calendario_externo set estado=case when calendario_externo_id is null
                    then 'PENDIENTE_AUTORIZACION' else 'CONECTADO' end,ultimo_error=null,
                    updated_at=current_timestamp,version=version+1 where empresa_id=:empresa and proveedor=:proveedor
                """).param("empresa",u.empresaId().toString()).param("proveedor",PROVEEDOR).update();
        encolarOcurrenciasPendientes(u.empresaId());
        return resumenColaInterno(u.empresaId());
    }

    @Transactional(readOnly = true)
    public List<EstadoOcurrencia> listarOcurrencias() {
        CurrentUser u=context.requirePermission("SANIDAD_VER");
        return jdbc.sql("""
                select o.id,o.fecha_prevista,coalesce(nullif(i.nombre,''),i.tipo_actividad) actividad,
                       p.nombre propiedad,po.nombre potrero,l.nombre lote,
                       (select count(*) from evento_calendario_sanitario e where e.ocurrencia_id=o.id) animales,
                       c.estado estado_externo,c.enlace_externo,c.ultima_sincronizacion,
                       (select q.estado from cola_sincronizacion_calendario q where q.ocurrencia_id=o.id
                        order by q.created_at desc limit 1) estado_cola,
                       (select q.ultimo_error from cola_sincronizacion_calendario q where q.ocurrencia_id=o.id
                        order by q.created_at desc limit 1) error
                from ocurrencia_calendario_sanitario o
                left join plan_sanitario_item i on i.id=o.plan_item_id
                left join propiedad p on p.id=o.propiedad_id left join potrero po on po.id=o.potrero_id
                left join lote_ganadero l on l.id=o.lote_ganadero_id
                left join correspondencia_calendario_externo c on c.ocurrencia_id=o.id and c.proveedor=:proveedor
                order by o.fecha_prevista desc limit 200
                """).param("proveedor",PROVEEDOR).query((r,n)->new EstadoOcurrencia(
                        UUID.fromString(r.getString("id")),r.getString("actividad"),Instant.parse(r.getString("fecha_prevista")),
                        r.getString("propiedad"),r.getString("potrero"),r.getString("lote"),r.getInt("animales"),
                        r.getString("estado_externo"),r.getString("estado_cola"),r.getString("enlace_externo"),
                        instant(r.getString("ultima_sincronizacion")),r.getString("error"))).list();
    }

    @Transactional
    public void reintentarOcurrencia(UUID ocurrenciaId) {
        CurrentUser u=context.requirePermission(PERMISO);
        Integer existe=jdbc.sql("select count(*) from ocurrencia_calendario_sanitario where id=:id")
                .param("id",ocurrenciaId.toString()).query(Integer.class).single();
        if(existe==0) throw new BusinessException(ErrorCode.COLA_CALENDARIO_TRABAJO_NOT_FOUND);
        jdbc.sql("update cola_sincronizacion_calendario set estado='CANCELADO',updated_at=current_timestamp where ocurrencia_id=:id and estado in ('PENDIENTE','REINTENTO','PROCESANDO','ERROR_DEFINITIVO')")
                .param("id",ocurrenciaId.toString()).update();
        boolean mapeado=jdbc.sql("select exists(select 1 from correspondencia_calendario_externo where ocurrencia_id=:id and proveedor=:p and estado<>'ELIMINADO')")
                .param("id",ocurrenciaId.toString()).param("p",PROVEEDOR).query(Boolean.class).single();
        String operacion=mapeado?"ACTUALIZAR":"CREAR";
        jdbc.sql("""
                insert into cola_sincronizacion_calendario(id,empresa_id,ocurrencia_id,proveedor,operacion,clave_idempotencia,payload)
                values(:id,:empresa,:ocurrencia,:proveedor,:operacion,:clave,'{}')
                """).param("id",UUID.randomUUID().toString()).param("empresa",u.empresaId().toString())
                .param("ocurrencia",ocurrenciaId.toString()).param("proveedor",PROVEEDOR).param("operacion",operacion)
                .param("clave",PROVEEDOR+":"+operacion+":"+ocurrenciaId+":manual:"+UUID.randomUUID()).update();
    }

    @Transactional
    public TrabajoSincronizacionCalendario completar(UUID trabajoId, ResultadoExterno resultado) {
        CurrentUser u = context.requirePermission(PERMISO);
        TrabajoSincronizacionCalendario trabajo = exigirProcesando(u.empresaId(), trabajoId);
        if (resultado.eventoExternoId() == null || resultado.eventoExternoId().isBlank())
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        Instant ahora = Instant.now();
        String estadoCorrespondencia = trabajo.operacion()==OperacionCalendario.CANCELAR || trabajo.operacion()==OperacionCalendario.ELIMINAR
                ? "ELIMINADO" : "SINCRONIZADO";
        jdbc.sql("""
                insert into correspondencia_calendario_externo
                    (id,empresa_id,ocurrencia_id,proveedor,evento_externo_id,etag,enlace_externo,estado,
                     version_local_sincronizada,fecha_actualizacion_externa,ultima_sincronizacion)
                values(:id,:empresa,:ocurrencia,:proveedor,:evento,:etag,:enlace,:estado,:versionLocal,:externa,:ahora)
                on conflict(ocurrencia_id,proveedor) do update set evento_externo_id=excluded.evento_externo_id,
                    etag=excluded.etag,enlace_externo=excluded.enlace_externo,estado=excluded.estado,
                    version_local_sincronizada=excluded.version_local_sincronizada,
                    fecha_actualizacion_externa=excluded.fecha_actualizacion_externa,
                    ultima_sincronizacion=excluded.ultima_sincronizacion,updated_at=current_timestamp,version=version+1
                """).param("id", UUID.randomUUID().toString()).param("empresa", u.empresaId().toString())
                .param("ocurrencia", trabajo.ocurrenciaId().toString()).param("proveedor", PROVEEDOR)
                .param("evento", resultado.eventoExternoId().trim()).param("etag", limpio(resultado.etag()))
                .param("estado",estadoCorrespondencia).param("versionLocal",trabajo.version())
                .param("enlace", limpio(resultado.enlaceExterno())).param("externa", resultado.fechaActualizacionExterna())
                .param("ahora", ahora.toString()).update();
        jdbc.sql("""
                update cola_sincronizacion_calendario set estado='COMPLETADO',completado_at=:ahora,
                    bloqueado_por=null,bloqueado_hasta=null,ultimo_error=null,codigo_error=null,
                    updated_at=current_timestamp,version=version+1 where id=:id
                """).param("ahora", ahora.toString()).param("id", trabajoId.toString()).update();
        jdbc.sql("""
                update configuracion_calendario_externo set estado='CONECTADO',ultimo_error=null,
                    ultima_sincronizacion=:ahora,updated_at=current_timestamp,version=version+1
                where empresa_id=:empresa and proveedor=:proveedor
                """).param("ahora", ahora.toString()).param("empresa", u.empresaId().toString())
                .param("proveedor", PROVEEDOR).update();
        return trabajo(u.empresaId(), trabajoId);
    }

    @Transactional
    public TrabajoSincronizacionCalendario fallar(UUID trabajoId, FalloSincronizacion fallo) {
        CurrentUser u = context.requirePermission(PERMISO);
        TrabajoSincronizacionCalendario trabajo = exigirProcesando(u.empresaId(), trabajoId);
        boolean definitivo = !fallo.reintentable() || trabajo.intentos() >= trabajo.maxIntentos();
        long espera = Math.min(3600, 30L * (1L << Math.min(7, Math.max(0, trabajo.intentos() - 1))));
        jdbc.sql("""
                update cola_sincronizacion_calendario set estado=:estado,proximo_intento=:proximo,
                    bloqueado_por=null,bloqueado_hasta=null,ultimo_error=:error,codigo_error=:codigo,
                    updated_at=current_timestamp,version=version+1 where id=:id
                """).param("estado", definitivo ? "ERROR_DEFINITIVO" : "REINTENTO")
                .param("proximo", Instant.now().plusSeconds(espera).toString())
                .param("error", abreviar(fallo.mensaje(), 2000)).param("codigo", abreviar(fallo.codigo(), 100))
                .param("id", trabajoId.toString()).update();
        jdbc.sql("""
                update configuracion_calendario_externo set estado=:estado,ultimo_error=:error,
                    updated_at=current_timestamp,version=version+1
                where empresa_id=:empresa and proveedor=:proveedor
                """).param("estado",definitivo ? "ERROR" : "CONECTADO")
                .param("error", abreviar(fallo.mensaje(), 2000)).param("empresa", u.empresaId().toString())
                .param("proveedor", PROVEEDOR).update();
        return trabajo(u.empresaId(), trabajoId);
    }

    private ConfiguracionCalendarioExterno buscarConfiguracion(UUID empresaId) {
        return jdbc.sql("""
                select * from configuracion_calendario_externo where empresa_id=:empresa and proveedor=:proveedor
                """).param("empresa", empresaId.toString()).param("proveedor", PROVEEDOR)
                .query((r,n) -> new ConfiguracionCalendarioExterno(UUID.fromString(r.getString("id")), empresaId,
                        r.getString("proveedor"),r.getString("cuenta_email"),r.getString("calendario_externo_id"),
                        r.getString("calendario_nombre"),r.getString("zona_horaria"),r.getBoolean("sincronizacion_automatica"),
                        EstadoConexionCalendario.valueOf(r.getString("estado")),r.getString("ultimo_error"),
                        instant(r.getString("ultima_sincronizacion")),r.getLong("version"))).optional()
                .orElseGet(() -> new ConfiguracionCalendarioExterno(UUID.randomUUID(), empresaId, PROVEEDOR, null,
                        null,"Ganadero - Sanidad","America/La_Paz",false,EstadoConexionCalendario.DESCONECTADO,null,null,0));
    }

    private void encolarOcurrenciasPendientes(UUID empresaId) {
        jdbc.sql("""
                insert into cola_sincronizacion_calendario
                    (id,empresa_id,ocurrencia_id,proveedor,operacion,clave_idempotencia,payload)
                select lower(hex(randomblob(4)))||'-'||lower(hex(randomblob(2)))||'-4'||substr(lower(hex(randomblob(2))),2)||
                       '-'||substr('89ab',abs(random())%4+1,1)||substr(lower(hex(randomblob(2))),2)||'-'||lower(hex(randomblob(6))),
                       :empresa,o.id,:proveedor,'CREAR',:proveedor||':CREAR:'||o.id||':v'||o.version,
                       json_object('ocurrenciaId',o.id,'actividadId',o.plan_item_id,'fechaPrevista',o.fecha_prevista,
                                   'propiedadId',o.propiedad_id,'potreroId',o.potrero_id,'loteId',o.lote_ganadero_id)
                from ocurrencia_calendario_sanitario o
                left join correspondencia_calendario_externo c on c.ocurrencia_id=o.id and c.proveedor=:proveedor
                where c.id is null
                on conflict(clave_idempotencia) do nothing
                """).param("empresa", empresaId.toString()).param("proveedor", PROVEEDOR).update();
    }

    /** Retira de Google las ocurrencias que ya no representan trabajo sanitario pendiente. */
    private void encolarDesenlaces(UUID empresaId) {
        jdbc.sql("""
                insert into cola_sincronizacion_calendario
                    (id,empresa_id,ocurrencia_id,proveedor,operacion,clave_idempotencia,payload)
                select lower(hex(randomblob(4)))||'-'||lower(hex(randomblob(2)))||'-4'||substr(lower(hex(randomblob(2))),2)||
                       '-'||substr('89ab',abs(random())%4+1,1)||substr(lower(hex(randomblob(2))),2)||'-'||lower(hex(randomblob(6))),
                       :empresa,o.id,:proveedor,'CANCELAR',:proveedor||':CANCELAR:'||o.id||':v'||o.version,'{}'
                from ocurrencia_calendario_sanitario o
                join correspondencia_calendario_externo c on c.ocurrencia_id=o.id and c.proveedor=:proveedor
                where c.estado<>'ELIMINADO'
                  and exists(select 1 from evento_calendario_sanitario e where e.ocurrencia_id=o.id)
                  and not exists(select 1 from evento_calendario_sanitario e where e.ocurrencia_id=o.id
                                 and e.estado not in ('REALIZADO','CANCELADO','OMITIDO'))
                  and not exists(select 1 from cola_sincronizacion_calendario q where q.ocurrencia_id=o.id
                                 and q.operacion in ('CANCELAR','ELIMINAR') and q.estado in ('PENDIENTE','PROCESANDO','REINTENTO','COMPLETADO'))
                on conflict(clave_idempotencia) do nothing
                """).param("empresa",empresaId.toString()).param("proveedor",PROVEEDOR).update();
    }

    private void liberarBloqueosExpirados(UUID empresaId) {
        jdbc.sql("""
                update cola_sincronizacion_calendario set estado='REINTENTO',bloqueado_por=null,bloqueado_hasta=null,
                    proximo_intento=:ahora,updated_at=current_timestamp,version=version+1
                where empresa_id=:empresa and estado='PROCESANDO' and bloqueado_hasta<:ahora
                """).param("empresa", empresaId.toString()).param("ahora", Instant.now().toString()).update();
    }

    private TrabajoSincronizacionCalendario exigirProcesando(UUID empresaId, UUID id) {
        TrabajoSincronizacionCalendario t = trabajo(empresaId,id);
        if (t.estado()!=EstadoColaCalendario.PROCESANDO) throw new BusinessException(ErrorCode.COLA_CALENDARIO_ESTADO_INVALIDO);
        return t;
    }

    private List<TrabajoSincronizacionCalendario> trabajos(UUID empresaId, List<String> ids) {
        if (ids.isEmpty()) return List.of();
        return jdbc.sql("""
                select q.*,
                  json_patch(coalesce(q.payload,'{}'),json_object(
                    'nombreActividad',coalesce(nullif(i.nombre,''),i.tipo_actividad),
                    'tipoActividad',i.tipo_actividad,'producto',i.producto_recomendado_texto,
                    'dosis',i.dosis_cantidad,'unidadDosis',i.dosis_unidad,
                    'via',coalesce(i.via_administracion_codigo,i.via_administracion),
                    'lugarAplicacion',i.lugar_aplicacion,'instrucciones',i.instrucciones_veterinario,
                    'diasAlerta',i.dias_alerta,'horariosAviso',json(coalesce(i.horarios_aviso,'[]')),
                    'propiedad',p.nombre,'potrero',po.nombre,'lote',l.nombre,
                    'animales',(select count(*) from evento_calendario_sanitario e where e.ocurrencia_id=o.id),
                    'eventoExternoId',c.evento_externo_id,'etag',c.etag,'enlaceExterno',c.enlace_externo
                  )) payload_completo
                from cola_sincronizacion_calendario q
                join ocurrencia_calendario_sanitario o on o.id=q.ocurrencia_id
                left join plan_sanitario_item i on i.id=o.plan_item_id
                left join propiedad p on p.id=o.propiedad_id
                left join potrero po on po.id=o.potrero_id
                left join lote_ganadero l on l.id=o.lote_ganadero_id
                left join correspondencia_calendario_externo c on c.ocurrencia_id=o.id and c.proveedor=q.proveedor
                where q.empresa_id=:empresa and q.id in (:ids) and q.estado='PROCESANDO' order by q.created_at
                """)
                .param("empresa",empresaId.toString()).param("ids",ids).query(this::mapTrabajo).list();
    }

    private TrabajoSincronizacionCalendario trabajo(UUID empresaId, UUID id) {
        return jdbc.sql("select * from cola_sincronizacion_calendario where empresa_id=:empresa and id=:id")
                .param("empresa",empresaId.toString()).param("id",id.toString()).query(this::mapTrabajo).optional()
                .orElseThrow(() -> new BusinessException(ErrorCode.COLA_CALENDARIO_TRABAJO_NOT_FOUND));
    }

    private TrabajoSincronizacionCalendario mapTrabajo(java.sql.ResultSet r, int n) throws java.sql.SQLException {
        return new TrabajoSincronizacionCalendario(UUID.fromString(r.getString("id")),UUID.fromString(r.getString("ocurrencia_id")),
                OperacionCalendario.valueOf(r.getString("operacion")),r.getString("clave_idempotencia"),
                hasColumn(r,"payload_completo") ? r.getString("payload_completo") : r.getString("payload"),
                EstadoColaCalendario.valueOf(r.getString("estado")),r.getInt("intentos"),r.getInt("max_intentos"),
                instant(r.getString("proximo_intento")),instant(r.getString("bloqueado_hasta")),r.getString("ultimo_error"),r.getLong("version"));
    }

    private static Instant instant(String value) { return value==null ? null : Instant.parse(value.replace(' ','T') + (value.endsWith("Z")?"":"Z")); }
    private static boolean hasColumn(java.sql.ResultSet r,String name) {
        try { r.findColumn(name); return true; } catch (java.sql.SQLException ignored) { return false; }
    }
    private static String limpio(String s) { return s==null||s.isBlank()?null:s.trim(); }
    private static String abreviar(String s,int max) { if(s==null)return null;return s.length()<=max?s:s.substring(0,max); }

    public record GuardarConfiguracion(String cuentaEmail,String calendarioNombre,String zonaHoraria,boolean sincronizacionAutomatica) {}
    public record ResultadoExterno(String eventoExternoId,String etag,String enlaceExterno,Instant fechaActualizacionExterna) {}
    public record FalloSincronizacion(String codigo,String mensaje,boolean reintentable) {}
    public record ConexionConfirmada(String cuentaEmail,String calendarioExternoId) {}
    public record AutorizacionConfirmada(String cuentaEmail) {}
    public record ConteoEstado(EstadoColaCalendario estado,long cantidad) {}
    public record ResumenCola(ConfiguracionCalendarioExterno configuracion,List<ConteoEstado> cola) {}
    public record EstadoOcurrencia(UUID ocurrenciaId,String actividad,Instant fechaPrevista,String propiedad,
                                  String potrero,String lote,int animales,String estadoExterno,String estadoCola,
                                  String enlaceExterno,Instant ultimaSincronizacion,String error) {}

    private ResumenCola resumenColaInterno(UUID empresaId) {
        List<ConteoEstado> conteos = jdbc.sql("select estado,count(*) cantidad from cola_sincronizacion_calendario where empresa_id=:empresa group by estado")
                .param("empresa",empresaId.toString()).query((r,n)->new ConteoEstado(EstadoColaCalendario.valueOf(r.getString("estado")),r.getLong("cantidad"))).list();
        return new ResumenCola(buscarConfiguracion(empresaId),conteos);
    }
}
