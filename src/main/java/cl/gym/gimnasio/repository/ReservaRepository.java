package cl.gym.gimnasio.repository;

import cl.gym.gimnasio.model.Reserva;
import cl.gym.gimnasio.model.Usuario;
import cl.gym.gimnasio.model.Clase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    // Verificar si ya existe una reserva para un socio en una clase específica
    boolean existsBySocioAndClase(Usuario socio, Clase clase);
}