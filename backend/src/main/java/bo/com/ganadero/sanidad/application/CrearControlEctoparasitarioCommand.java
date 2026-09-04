package bo.com.ganadero.sanidad.application;
import bo.com.ganadero.sanidad.domain.NivelCargaParasitaria;import bo.com.ganadero.sanidad.domain.TipoEctoparasito;import java.time.LocalDate;import java.util.UUID;
public record CrearControlEctoparasitarioCommand(UUID animalId,UUID loteGanaderoId,TipoEctoparasito tipo,NivelCargaParasitaria nivelCarga,boolean tratado,String producto,String principioActivo,LocalDate fecha,String observaciones){}
