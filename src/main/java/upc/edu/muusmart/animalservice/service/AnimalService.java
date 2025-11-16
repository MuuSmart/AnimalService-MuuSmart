package upc.edu.muusmart.animalservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upc.edu.muusmart.animalservice.model.Animal;
import upc.edu.muusmart.animalservice.repository.AnimalRepository;

import java.util.List;
import java.util.Optional;

/**
 * Service that encapsulates business logic for managing animals.
 *
 * <p>Provides methods to create, retrieve, update and delete animals. Additional
 * validation (e.g. checking ownership) can be added here in the future.</p>
 */
@Service
@RequiredArgsConstructor
public class AnimalService {

    private final AnimalRepository animalRepository;

    /**
     * Persists a new animal in the database.
     *
     * @param animal the animal to persist
     * @return the saved animal
     */
    public Animal createAnimal(Animal animal) {
        return animalRepository.save(animal);
    }

    /**
     * Retrieves an animal by its identifier.
     *
     * @param id the animal ID
     * @return an {@link Optional} containing the animal if found
     */
    public Optional<Animal> getAnimalById(Long id) {
        return animalRepository.findById(id);
    }

    /**
     * Retrieves all animals owned by a specific user.
     *
     * @param ownerUsername the owner's username
     * @return list of animals owned by the user
     */
    public List<Animal> getAnimalsByOwnerUsername(String ownerUsername) {
        return animalRepository.findByOwnerUsername(ownerUsername);
    }

    /**
     * Updates an existing animal.
     *
     * @param id the ID of the animal to update
     * @param updated the new data for the animal
     * @return the updated animal
     */
    @Transactional
    public Animal updateAnimal(Long id, Animal updated) {
        return animalRepository.findById(id).map(existing -> {
            existing.setTag(updated.getTag());
            existing.setBreed(updated.getBreed());
            existing.setWeight(updated.getWeight());
            existing.setAge(updated.getAge());
            existing.setStatus(updated.getStatus());
            existing.setOwnerUsername(updated.getOwnerUsername());
            existing.setFeedLevel(updated.getFeedLevel());
            return existing;
        }).orElseThrow(() -> new IllegalArgumentException("Animal not found"));
    }

    /**
     * Deletes an animal by its ID.
     *
     * @param id the animal ID
     */
    public void deleteAnimal(Long id) {
        animalRepository.deleteById(id);
    }

    // -------------------------------------------------------------------------
    // Access control aware methods
    //
    // These methods encapsulate permission checks so that callers can work
    // with domain objects without duplicating security logic. A user can
    // perform an operation on an animal if they own it (ownerUsername matches the
    // authenticated username) or if they have the ADMIN role.
    // -------------------------------------------------------------------------

    /**
     * Retrieves an animal by ID and ensures the requesting user has access.
     *
     * @param id      the animal ID
     * @param username the authenticated principal (subject)
     * @param isAdmin whether the user has the ADMIN role
     * @return the animal if accessible
     * @throws SecurityException if the user is not allowed to access the animal
     */
    public Animal getAnimalByIdForUser(Long id, String username, boolean isAdmin) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Animal not found"));
        if (animal.getOwnerUsername().equals(username) || isAdmin) {
            return animal;
        }
        throw new SecurityException("Access denied to animal with id " + id);
    }

    /**
     * Retrieves all animals visible to the given user. Administrators see all
     * animals; normal users see only their own.
     *
     * @param username the authenticated principal (subject)
     * @param isAdmin whether the user has the ADMIN role
     * @return a list of animals the user can view
     */
    public List<Animal> getAnimalsForUser(String username, boolean isAdmin) {
        if (isAdmin) {
            return animalRepository.findAll();
        }
        return animalRepository.findByOwnerUsername(username);
    }

    /**
     * Updates an animal after validating access rights. The owner username is not
     * modified by this operation unless it is explicitly set by an admin.
     *
     * @param id      the ID of the animal to update
     * @param updated new values for the animal
     * @param username the authenticated principal (subject)
     * @param isAdmin whether the user has the ADMIN role
     * @return the updated animal
     */
    @Transactional
    public Animal updateAnimalForUser(Long id, Animal updated, String username, boolean isAdmin) {
        Animal existing = animalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Animal not found"));
        boolean isOwner = existing.getOwnerUsername().equals(username);
        if (!isOwner && !isAdmin) {
            throw new SecurityException("Access denied to update animal with id " + id);
        }
        existing.setTag(updated.getTag());
        existing.setBreed(updated.getBreed());
        existing.setWeight(updated.getWeight());
        existing.setAge(updated.getAge());
        existing.setStatus(updated.getStatus());
        existing.setFeedLevel(updated.getFeedLevel());
        // Only admins can reassign ownership
        if (isAdmin && updated.getOwnerUsername() != null) {
            existing.setOwnerUsername(updated.getOwnerUsername());
        }
        return existing;
    }

    /**
     * Deletes an animal if the requesting user has sufficient privileges.
     *
     * @param id      the animal ID
     * @param username the authenticated principal (subject)
     * @param isAdmin whether the user has the ADMIN role
     */
    public void deleteAnimalForUser(Long id, String username, boolean isAdmin) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Animal not found"));
        if (animal.getOwnerUsername().equals(username) || isAdmin) {
            animalRepository.deleteById(id);
        } else {
            throw new SecurityException("Access denied to delete animal with id " + id);
        }
    }
}