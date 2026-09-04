package bo.com.ganadero.propiedades.application;
import bo.com.ganadero.propiedades.domain.*; import bo.com.ganadero.shared.codigos.*; import bo.com.ganadero.shared.error.*; import bo.com.ganadero.shared.security.*;
import org.springframework.context.ApplicationEventPublisher; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.time.Instant; import java.util.*;

@Service public class PropiedadService {
 private final PropiedadRepository propiedades; private final SectorRepository sectores; private final UserContext context; private final ApplicationEventPublisher events; private final CodigoService codigos;
 public PropiedadService(PropiedadRepository p,SectorRepository s,UserContext c,ApplicationEventPublisher e,CodigoService codes){propiedades=p;sectores=s;context=c;events=e;codigos=codes;}
 @Transactional(readOnly=true) public List<Propiedad> list(){context.requirePermission("PROPIEDAD_VER");return propiedades.findAll();}
 @Transactional(readOnly=true) public Propiedad get(UUID id){context.requirePermission("PROPIEDAD_VER");return require(id);}
 @Transactional public Propiedad create(PropiedadCommand c){
  CurrentUser u=context.requirePermission("PROPIEDAD_CREAR");
  String codigo=codigos.paraCreacion(u,TipoCodigo.PROPIEDAD,null,null,c.codigo());
  Propiedad p=new Propiedad(UUID.randomUUID(),codigo,c.nombre(),c.descripcion(),c.departamento(),c.municipio(),c.localidad(),c.direccionReferencia(),c.superficieHa(),c.ubicacionWkt(),c.limiteGeograficoWkt(),true,0);
  Propiedad saved=propiedades.create(p,u.userId());
  audit(u,"CREAR","PROPIEDAD",saved.id());
  return saved;
 }
 @Transactional public Propiedad update(UUID id,PropiedadCommand c){
  CurrentUser u=context.requirePermission("PROPIEDAD_EDITAR");
  Propiedad old=require(id);
  boolean active=c.activo()==null?old.activo():c.activo();
  if(old.activo()&&!active&&propiedades.hasActiveAnimals(id))throw new BusinessException(ErrorCode.PROPERTY_HAS_ACTIVE_ANIMALS);
  String codigo=codigos.paraActualizacion(u,TipoCodigo.PROPIEDAD,null,null,old.codigo(),c.codigo());
  Propiedad p=new Propiedad(id,codigo,c.nombre()==null?old.nombre():c.nombre(),c.descripcion()==null?old.descripcion():c.descripcion(),
   c.departamento()==null?old.departamento():c.departamento(),c.municipio()==null?old.municipio():c.municipio(),
   c.localidad()==null?old.localidad():c.localidad(),c.direccionReferencia()==null?old.direccionReferencia():c.direccionReferencia(),
   c.superficieHa()==null?old.superficieHa():c.superficieHa(),c.ubicacionWkt()==null?old.ubicacionWkt():c.ubicacionWkt(),
   c.limiteGeograficoWkt()==null?old.limiteGeograficoWkt():c.limiteGeograficoWkt(),active,Objects.requireNonNull(c.version()));
  Propiedad saved=propiedades.update(p,u.userId());
  audit(u,"ACTUALIZAR","PROPIEDAD",id);
  return saved;
 }
 @Transactional(readOnly=true) public List<Sector> sectors(UUID propiedadId){context.requirePermission("PROPIEDAD_VER");require(propiedadId);return sectores.findAll(propiedadId);}
 @Transactional public Sector createSector(UUID propiedadId,SectorCommand c){CurrentUser u=context.requirePermission("PROPIEDAD_CREAR");require(propiedadId);String codigo=codigos.paraCreacion(u,TipoCodigo.SECTOR,propiedadId,null,c.codigo());Sector saved=sectores.create(new Sector(UUID.randomUUID(),propiedadId,codigo,c.nombre(),c.descripcion(),true,0),u.userId());audit(u,"CREAR","SECTOR",saved.id());return saved;}
 @Transactional public Sector updateSector(UUID id,SectorCommand c){CurrentUser u=context.requirePermission("PROPIEDAD_EDITAR");Sector old=sectores.findSectorById(id).orElseThrow(()->new BusinessException(ErrorCode.SECTOR_NOT_FOUND));String codigo=codigos.paraActualizacion(u,TipoCodigo.SECTOR,old.propiedadId(),null,old.codigo(),c.codigo());Sector saved=sectores.update(new Sector(id,old.propiedadId(),codigo,c.nombre()==null?old.nombre():c.nombre(),c.descripcion()==null?old.descripcion():c.descripcion(),c.activo()==null?old.activo():c.activo(),Objects.requireNonNull(c.version())),u.userId());audit(u,"ACTUALIZAR","SECTOR",id);return saved;}
 private Propiedad require(UUID id){return propiedades.findById(id).orElseThrow(()->new BusinessException(ErrorCode.PROPERTY_NOT_FOUND));}
 private void audit(CurrentUser u,String a,String e,UUID id){events.publishEvent(new CampoAuditEvent(u.empresaId(),u.userId(),a,e,id,Instant.now()));}
}
