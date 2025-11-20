package upc.edu.muusmart.animalservice.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Data Transfer Object for creating or updating animals.
 *
 * <p>This class is used to capture input from API callers when they create
 * or update animal records. Validation annotations ensure that required
 * fields are present and conform to expected ranges.</p>
 */
@Data
public class AnimalRequest {

    @NotBlank
    private String tag;

    @NotBlank
    private String breed;

    @NotNull
    @Positive
    private Double weight;

    @NotNull
    @Positive
    private Integer age;

    @NotBlank
    private String status;

    /**
     * Level of feed (e.g. scale of 1-3). Must be positive.
     */
    @NotNull
    @Positive
    private Integer feedLevel;

    // Nuevo campo
    @NotNull(message = "StableId es requerido")
    private Long stableId;

}