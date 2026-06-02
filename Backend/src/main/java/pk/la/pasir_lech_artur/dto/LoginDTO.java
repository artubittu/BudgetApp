package pk.la.pasir_lech_artur.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginDTO {

    @NotBlank
    private String email;

    @NotBlank
    private String password;
}