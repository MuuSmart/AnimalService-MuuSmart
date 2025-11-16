package upc.edu.muusmart.animalservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a bovine in the Animal Management Service.
 *
 * <p>Each animal is assigned to a specific owner (referenced by ownerId) and
 * contains basic attributes such as tag, breed, weight and age. Additional
 * attributes like feedLevel can be added as needed.</p>
 */
@Entity
@Table(name = "animals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Animal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * A unique identifier for the animal, analogous to a tag or name.
     */
    @Column(unique = true, nullable = false)
    private String tag;

    /**
     * Breed of the animal (e.g. Holstein, Angus, etc.).
     */
    @Column(nullable = false)
    private String breed;

    /**
     * Current weight of the animal in kilograms.
     */
    @Column(nullable = false)
    private Double weight;

    /**
     * Age of the animal in years.
     */
    @Column(nullable = false)
    private Integer age;

    /**
     * Health or reproductive status (e.g. ACTIVE, INACTIVE, PREGNANT).
     */
    @Column(nullable = false)
    private String status;

    /**
     * Username of the user who owns this animal. This field stores the
     * principal name (subject) extracted from the JWT token issued by the
     * IAM microservice. It is not a foreign key, since Animal Service does
     * not maintain a local user table.
     */
    @Column(nullable = false)
    private String ownerUsername;

    /**
     * Level of feeding assigned to the animal (optional field for production calculations).
     */
    private Double feedLevel;
}