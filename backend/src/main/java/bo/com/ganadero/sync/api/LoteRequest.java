package bo.com.ganadero.sync.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record LoteRequest(@Valid @NotEmpty @Size(max = 2000) List<SyncPushRequest.OperacionRequest> operaciones) {
}
