package bo.com.ganadero.animales.domain; import java.util.*;
public interface RazaRepository {List<Raza> findActive(UUID empresa);Optional<Raza> findById(UUID id,UUID empresa);}
