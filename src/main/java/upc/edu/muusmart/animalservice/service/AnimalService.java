package upc.edu.muusmart.animalservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import upc.edu.muusmart.animalservice.model.Animal;
import upc.edu.muusmart.animalservice.repository.AnimalRepository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final RestTemplate restTemplate;
    private static final String STABLE_URL = "http://localhost:8080/stables/";
    public void verifyStableExists(Long stableId, String authHeader) {
        if (stableId == null) return;

        if (authHeader == null || authHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "🔐 Falta el token para validar establo");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(
                    STABLE_URL + stableId,
                    HttpMethod.GET,
                    entity,
                    Void.class
            );

        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "❌ El establo con ID " + stableId + " no existe");

        } catch (HttpClientErrorException.Forbidden e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "🚫 No tienes permisos para acceder a este establo");

        } catch (HttpClientErrorException.Unauthorized e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "🔐 Token inválido para validar establo");

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "⚠️ Error comunicando con servicio de establos");
        }
    }

    // -------------------------------------------------------------------------
    // 🐮 CREAR
    // -------------------------------------------------------------------------
    public Animal createAnimal(Animal animal) {
        String cleanOwner = clean(animal.getOwnerUsername());
        animal.setOwnerUsername(cleanOwner);

        log.info("🐮 [AnimalService] Creando animal para usuario '{}'", cleanOwner);
        return animalRepository.save(animal);
    }

    // -------------------------------------------------------------------------
    // 🐮 OBTENER POR ID (sin validación)
    // -------------------------------------------------------------------------
    public Optional<Animal> getAnimalById(Long id) {
        return animalRepository.findById(id);
    }

    // -------------------------------------------------------------------------
    // 🐮 OBTENER POR OWNER
    // -------------------------------------------------------------------------
    public List<Animal> getAnimalsByOwnerUsername(String ownerUsername) {
        return animalRepository.findByOwnerUsername(clean(ownerUsername));
    }

    // -------------------------------------------------------------------------
    // 🐮 UPDATE NORMAL
    // -------------------------------------------------------------------------
    @Transactional
    public Animal updateAnimal(Long id, Animal updated) {
        return animalRepository.findById(id).map(existing -> {
            existing.setTag(updated.getTag());
            existing.setBreed(updated.getBreed());
            existing.setWeight(updated.getWeight());
            existing.setAge(updated.getAge());
            existing.setStatus(updated.getStatus());
            existing.setFeedLevel(updated.getFeedLevel());
            existing.setOwnerUsername(clean(updated.getOwnerUsername()));
            return existing;
        }).orElseThrow(() -> new IllegalArgumentException("Animal not found"));
    }

    // -------------------------------------------------------------------------
    // 🐮 DELETE NORMAL
    // -------------------------------------------------------------------------
    public void deleteAnimal(Long id) {
        animalRepository.deleteById(id);
    }

    // -------------------------------------------------------------------------
    // 🛡️ MÉTODOS CON VALIDACIÓN DE ACCESO
    // -------------------------------------------------------------------------

    /**
     * Limpieza segura del username
     */
    private String clean(String str) {
        if (str == null) return null;
        return str.trim()
                .replace("\u200B", "") // quita zero-width space
                .toLowerCase();        // comparación insensible a mayúsculas
    }

    /**
     * Accede al animal y valida permisos.
     */
    public Animal getAnimalByIdForUser(Long id, String username, boolean isAdmin) {

        String cleanUser = clean(username);

        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Animal not found"));

        String cleanOwner = clean(animal.getOwnerUsername());

        log.info("🔎 Validando acceso animal {} | owner='{}' | user='{}' | admin={}",
                id, cleanOwner, cleanUser, isAdmin);

        if (cleanOwner.equals(cleanUser) || isAdmin) {
            log.info("✅ Acceso permitido");
            return animal;
        }

        log.warn("❌ Acceso denegado: owner='{}' user='{}'", cleanOwner, cleanUser);
        throw new SecurityException("No tiene permiso para acceder al animal");
    }

    /**
     * Lista todos los animales accesibles para el usuario.
     */
    public List<Animal> getAnimalsForUser(String username, boolean isAdmin) {
        String cleanUser = clean(username);

        if (isAdmin) {
            log.info("👑 Admin solicitando lista completa de animales");
            return animalRepository.findAll();
        }

        log.info("👤 Usuario '{}' solicitando solo sus animales", cleanUser);
        return animalRepository.findByOwnerUsername(cleanUser);
    }

    /**
     * Update con validación.
     */
    @Transactional
    public Animal updateAnimalForUser(Long id, Animal updated, String username, boolean isAdmin) {

        String cleanUser = clean(username);

        Animal existing = animalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Animal not found"));

        String cleanOwner = clean(existing.getOwnerUsername());

        boolean isOwner = cleanOwner.equals(cleanUser);

        log.info("✏️ Update animal {} | owner='{}' user='{}' | admin={}",
                id, cleanOwner, cleanUser, isAdmin);

        if (!isOwner && !isAdmin) {
            log.warn("❌ Update denegado");
            throw new SecurityException("No tiene permiso para actualizar este animal");
        }

        existing.setTag(updated.getTag());
        existing.setBreed(updated.getBreed());
        existing.setWeight(updated.getWeight());
        existing.setAge(updated.getAge());
        existing.setStatus(updated.getStatus());
        existing.setFeedLevel(updated.getFeedLevel());

        if (isAdmin && updated.getOwnerUsername() != null) {
            existing.setOwnerUsername(clean(updated.getOwnerUsername()));
        }

        return existing;
    }

    public List<Animal> getAnimalsByStableForUser(Long stableId, String username, boolean isAdmin) {
        if (isAdmin) {
            return animalRepository.findByStableId(stableId);
        }
        return animalRepository.findByStableIdAndOwnerUsername(stableId, username);
    }


    /**
     * Delete con validación.
     */
    public void deleteAnimalForUser(Long id, String username, boolean isAdmin) {

        String cleanUser = clean(username);

        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Animal not found"));

        String cleanOwner = clean(animal.getOwnerUsername());

        if (cleanOwner.equals(cleanUser) || isAdmin) {
            log.info("🗑️ Eliminando animal {}", id);
            animalRepository.deleteById(id);
        } else {
            log.warn("❌ Delete denegado owner='{}', user='{}'", cleanOwner, cleanUser);
            throw new SecurityException("No tiene permiso para eliminar el animal");
        }
    }
}
