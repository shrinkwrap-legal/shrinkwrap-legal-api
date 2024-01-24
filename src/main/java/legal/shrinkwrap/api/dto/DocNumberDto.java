package legal.shrinkwrap.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DocNumberDto(@NotNull @NotBlank @Size(max = 50) String docNumber) {
}
