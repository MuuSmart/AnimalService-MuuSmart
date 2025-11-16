package upc.edu.muusmart.animalservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import upc.edu.muusmart.animalservice.model.Animal;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for performing CRUD operations on {@link Animal} entities.
 */
public interface AnimalRepository extends JpaRepository<Animal, Long> {
    Optional<Animal> findByTag(String tag);
    List<Animal> findByOwnerUsername(String ownerUsername);
}