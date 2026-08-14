package bo.com.ganadero.sync.api;

import bo.com.ganadero.sync.api.SyncPushRequest.OperacionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record LoteRequest(@NotEmpty @Size(max = 2000) List<@Valid OperacionRequest> operaciones) {
}
