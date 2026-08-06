package bo.com.ganadero.seguridad.api;import jakarta.validation.constraints.NotNull;import java.util.*;
public record IdsRequest(@NotNull Set<UUID> ids,@NotNull Long version){public IdsRequest{ids=ids==null?Set.of():Set.copyOf(ids);}}
