package bo.com.ganadero.sanidad.domain;import java.time.LocalDate;import java.util.UUID;
public record ControlEctoparasitario(UUID id,UUID empresaId,UUID animalId,UUID loteGanaderoId,TipoEctoparasito tipo,NivelCargaParasitaria nivelCarga,boolean tratado,String producto,String principioActivo,LocalDate fecha,String observaciones,long version){}
