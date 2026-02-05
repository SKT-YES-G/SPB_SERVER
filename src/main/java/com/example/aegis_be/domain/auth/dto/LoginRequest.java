package com.example.aegis_be.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "소방서명은 필수입니다")
    private String name;

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;
}
