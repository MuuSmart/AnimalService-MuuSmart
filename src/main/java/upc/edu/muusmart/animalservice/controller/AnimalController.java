package upc.edu.muusmart.animalservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import upc.edu.muusmart.animalservice.model.Animal;
// no local AppUser in this service; owner is stored as username in the token
import upc.edu.muusmart.animalservice.payload.AnimalRequest;
import upc.edu.muusmart.animalservice.service.AnimalService;

import java.util.List;

/**
 * REST controller exposing endpoints for managing animals.
 *
 * <p>All endpoints require authentication. Normal users can create and
 * manage their own animals. Administrators can manage all animals.</p>
 */
@RestController
@RequestMapping("/animals")
@RequiredArgsConstructor
public class AnimalController {

    private final AnimalService animalService;

    /**
     * Creates a new animal associated with the authenticated user.
     *
     * @param request  the request body containing animal data
     * @param principal the authenticated user principal
     * @return the created animal
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping
    public ResponseEntity<Animal> createAnimal(@Valid @RequestBody AnimalRequest request,
                                               @AuthenticationPrincipal Object principal) {
        // Extract the username from the security context. In this service, the
        // principal may be a String (subject) rather than a full UserDetails.
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();

        Animal animal = new Animal();
        animal.setTag(request.getTag());
        animal.setBreed(request.getBreed());
        animal.setWeight(request.getWeight());
        animal.setAge(request.getAge());
        animal.setStatus(request.getStatus());
        // Convert feedLevel from Integer to Double
        animal.setFeedLevel(request.getFeedLevel() != null ? request.getFeedLevel().doubleValue() : null);
        animal.setOwnerUsername(username);
        Animal saved = animalService.createAnimal(animal);
        return ResponseEntity.ok(saved);
    }

    /**
     * Retrieves a specific animal if accessible by the current user.
     *
     * @param id        the animal ID
     * @param principal the authenticated user
     * @return the animal data
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<Animal> getAnimal(@PathVariable Long id,
                                            @AuthenticationPrincipal Object principal) {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Animal animal = animalService.getAnimalByIdForUser(id, username, isAdmin);
        return ResponseEntity.ok(animal);
    }

    /**
     * Retrieves all animals visible to the current user. Administrators see
     * all animals; normal users see only their own.
     *
     * @param principal the authenticated user
     * @return list of animals
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping
    public ResponseEntity<List<Animal>> getAnimals(@AuthenticationPrincipal Object principal) {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        List<Animal> animals = animalService.getAnimalsForUser(username, isAdmin);
        return ResponseEntity.ok(animals);
    }

    /**
     * Updates an animal record. Users can update only their own animals;
     * administrators can update any animal.
     *
     * @param id        the animal ID
     * @param request   the new data for the animal
     * @param principal the authenticated user
     * @return the updated animal
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Animal> updateAnimal(@PathVariable Long id,
                                               @Valid @RequestBody AnimalRequest request,
                                               @AuthenticationPrincipal Object principal) {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Animal updated = new Animal();
        updated.setTag(request.getTag());
        updated.setBreed(request.getBreed());
        updated.setWeight(request.getWeight());
        updated.setAge(request.getAge());
        updated.setStatus(request.getStatus());
        updated.setFeedLevel(request.getFeedLevel() != null ? request.getFeedLevel().doubleValue() : null);
        Animal result = animalService.updateAnimalForUser(id, updated, username, isAdmin);
        return ResponseEntity.ok(result);
    }

    /**
     * Deletes an animal. Users can delete only their own animals; administrators
     * can delete any animal.
     *
     * @param id        the animal ID
     * @param principal the authenticated user
     * @return HTTP 204 on success
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnimal(@PathVariable Long id,
                                             @AuthenticationPrincipal Object principal) {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        animalService.deleteAnimalForUser(id, username, isAdmin);
        return ResponseEntity.noContent().build();
    }
}