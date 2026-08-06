package bo.com.ganadero.potreros.domain; import java.util.*;
public interface TipoPastoRepository { List<TipoPasto> findActive(UUID empresa); }
