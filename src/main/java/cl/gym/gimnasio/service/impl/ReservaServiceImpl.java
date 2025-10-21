package cl.gym.gimnasio.service.impl;

import cl.gym.gimnasio.dto.ReservaDTO;
import cl.gym.gimnasio.dto.request.CreateReservaRequest;
import cl.gym.gimnasio.dto.response.CreateReservaResponse;
import cl.gym.gimnasio.mapper.ReservaMapper;
import cl.gym.gimnasio.model.Clase;
import cl.gym.gimnasio.model.Reserva;
import cl.gym.gimnasio.model.Usuario;
import cl.gym.gimnasio.repository.ClaseRepository;
import cl.gym.gimnasio.repository.ReservaRepository;
import cl.gym.gimnasio.repository.UsuarioRepository;
import cl.gym.gimnasio.service.ReservaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClaseRepository claseRepository;

    // Metodo para listar las reservas
    @Override
    @Transactional(readOnly = true)
    public List<Reserva> getAllReservas(){
        System.out.println("🟡 Obteniendo todas las reservas");
        return reservaRepository.findAll();
    }

    // Metodo para buscar reserva por id
    @Override
    @Transactional(readOnly = true)
    public ReservaDTO getReservaPorId(Integer id) {
        System.out.println("🟡 Buscando reserva ID: " + id);

        // CAMBIADO: findById en lugar de getReferenceById
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada con ID: " + id));

        System.out.println("✅ Reserva encontrada ID: " + reserva.getIdReserva());
        return ReservaMapper.modelToDTO(reserva);
    }

    // Metodo para crear reserva
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateReservaResponse createReserva(CreateReservaRequest createReservaRequest) throws Exception {
        System.out.println("🟡 Iniciando creación de reserva para clase: " + createReservaRequest.getClase());

        // Validaciones
        validarReservaRequest(createReservaRequest);

        // Obtener entidades relacionadas
        Usuario socio = usuarioRepository.findById(createReservaRequest.getSocio())
                .orElseThrow(() -> new Exception("Socio no encontrado con ID: " + createReservaRequest.getSocio()));

        Clase clase = claseRepository.findById(createReservaRequest.getClase())
                .orElseThrow(() -> new Exception("Clase no encontrada con ID: " + createReservaRequest.getClase()));

        // Validar que el socio no sea un coach
        if ("coach".equals(socio.getRol())) {
            throw new Exception("Los coaches no pueden hacer reservas como socios");
        }

        // Validar cupo disponible
        if (("confirmado".equals(createReservaRequest.getEstado()) ||
                "pendiente".equals(createReservaRequest.getEstado())) &&
                clase.getCupoDisponible() <= 0) {
            throw new Exception("No hay cupos disponibles para esta clase");
        }

        // Verificar si ya existe una reserva para este socio en esta clase
        if (reservaRepository.existsBySocioAndClase(socio, clase)) {
            throw new Exception("Ya tienes una reserva para esta clase");
        }

        // Mapear request a modelo
        Reserva reserva = ReservaMapper.createRequestToModel(createReservaRequest);
        reserva.setSocio(socio);
        reserva.setClase(clase);

        System.out.println("🟡 Guardando reserva en BD...");

        // Guardar reserva
        Reserva reservaGuardada = reservaRepository.save(reserva);
        reservaRepository.flush(); // Forzar flush para detectar errores

        // ACTUALIZAR CUPO DISPONIBLE si el estado ocupa cupo
        if ("confirmado".equals(createReservaRequest.getEstado()) ||
                "pendiente".equals(createReservaRequest.getEstado())) {
            clase.setCupoDisponible(clase.getCupoDisponible() - 1);
            claseRepository.save(clase);
            claseRepository.flush();
            System.out.println("🟡 Cupo actualizado. Disponible: " + clase.getCupoDisponible());
        }

        System.out.println("✅ Reserva creada exitosamente. ID: " + reservaGuardada.getIdReserva());

        return ReservaMapper.modelToResponse(reservaGuardada);
    }

    // Metodo para actualizar la reserva
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateReservaResponse updateReserva(Integer id, CreateReservaRequest createReservaRequest) throws Exception {
        System.out.println("🟡 Actualizando reserva ID: " + id);

        // Verificamos que exista
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new Exception("Reserva no encontrada"));

        // Guardar estado anterior para lógica de cupos
        String estadoAnterior = reserva.getEstado();
        Clase claseAnterior = reserva.getClase();

        // Validaciones
        validarReservaRequest(createReservaRequest);

        // Obtener entidades relacionadas
        Usuario socio = usuarioRepository.findById(createReservaRequest.getSocio())
                .orElseThrow(() -> new Exception("Socio no encontrado con ID: " + createReservaRequest.getSocio()));

        Clase clase = claseRepository.findById(createReservaRequest.getClase())
                .orElseThrow(() -> new Exception("Clase no encontrada con ID: " + createReservaRequest.getClase()));

        // LÓGICA DE ACTUALIZACIÓN DE CUPOS
        String nuevoEstado = createReservaRequest.getEstado();

        // Solo procesar cambios de cupo si cambió el estado
        if (!estadoAnterior.equals(nuevoEstado)) {
            System.out.println("🟡 Cambio de estado: " + estadoAnterior + " → " + nuevoEstado);

            // Caso 1: De pendiente/confirmado a cancelado - LIBERAR CUPO
            if (("pendiente".equals(estadoAnterior) || "confirmado".equals(estadoAnterior))
                    && "cancelado".equals(nuevoEstado)) {

                // Liberar cupo en la clase anterior
                claseAnterior.setCupoDisponible(claseAnterior.getCupoDisponible() + 1);
                claseRepository.save(claseAnterior);
                claseRepository.flush();
                System.out.println("🟡 Cupo liberado. Disponible: " + claseAnterior.getCupoDisponible());
            }
            // Caso 2: De cancelado a pendiente/confirmado - OCUPAR CUPO
            else if ("cancelado".equals(estadoAnterior)
                    && ("pendiente".equals(nuevoEstado) || "confirmado".equals(nuevoEstado))) {

                // Validar que haya cupo disponible
                if (clase.getCupoDisponible() <= 0) {
                    throw new Exception("No hay cupos disponibles para confirmar esta reserva");
                }

                // Ocupar cupo en la nueva clase
                clase.setCupoDisponible(clase.getCupoDisponible() - 1);
                claseRepository.save(clase);
                claseRepository.flush();
                System.out.println("🟡 Cupo ocupado. Disponible: " + clase.getCupoDisponible());
            }
            // Caso 3: Cambio de clase - manejar cupos entre clases
            else if (!claseAnterior.getIdClase().equals(clase.getIdClase())) {
                System.out.println("🟡 Cambio de clase detectado");

                // Liberar cupo en clase anterior si estaba ocupado
                if ("pendiente".equals(estadoAnterior) || "confirmado".equals(estadoAnterior)) {
                    claseAnterior.setCupoDisponible(claseAnterior.getCupoDisponible() + 1);
                    claseRepository.save(claseAnterior);
                    System.out.println("🟡 Cupo liberado en clase anterior. Disponible: " + claseAnterior.getCupoDisponible());
                }

                // Ocupar cupo en nueva clase si el nuevo estado ocupa cupo
                if ("pendiente".equals(nuevoEstado) || "confirmado".equals(nuevoEstado)) {
                    if (clase.getCupoDisponible() <= 0) {
                        throw new Exception("No hay cupos disponibles en la nueva clase");
                    }
                    clase.setCupoDisponible(clase.getCupoDisponible() - 1);
                    claseRepository.save(clase);
                    System.out.println("🟡 Cupo ocupado en nueva clase. Disponible: " + clase.getCupoDisponible());
                }

                claseRepository.flush();
            }
            // Caso 4: Cambio entre pendiente y confirmado (no afecta cupos)
            else {
                System.out.println("🟡 Cambio entre pendiente/confirmado - sin afectar cupos");
            }
        }

        // Actualizar campos de la entidad existente
        reserva.setFechaReserva(createReservaRequest.getFechaReserva());
        reserva.setEstado(createReservaRequest.getEstado());

        // Actualizar entidades relacionadas
        reserva.setSocio(socio);
        reserva.setClase(clase);

        System.out.println("🟡 Guardando cambios de reserva...");

        // Guardar cambios en db
        Reserva reservaActualizada = reservaRepository.save(reserva);
        reservaRepository.flush();

        System.out.println("✅ Reserva actualizada exitosamente. ID: " + reservaActualizada.getIdReserva());

        return ReservaMapper.modelToResponse(reservaActualizada);
    }

    // Metodo para eliminar reserva
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReserva(Integer id) throws Exception {
        System.out.println("🟡 Eliminando reserva ID: " + id);

        // Verificamos que exista la reserva
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new Exception("La reserva no existe"));

        // LIBERAR CUPO si la reserva estaba en estado que ocupa cupo
        if ("pendiente".equals(reserva.getEstado()) || "confirmado".equals(reserva.getEstado())) {
            Clase clase = reserva.getClase();
            clase.setCupoDisponible(clase.getCupoDisponible() + 1);
            claseRepository.save(clase);
            claseRepository.flush();
            System.out.println("🟡 Cupo liberado al eliminar. Disponible: " + clase.getCupoDisponible());
        }

        // Eliminamos
        reservaRepository.deleteById(id);
        System.out.println("✅ Reserva eliminada exitosamente. ID: " + id);
    }

    private void validarReservaRequest(CreateReservaRequest request) throws Exception {
        if(request == null){
            throw new Exception("La reserva no puede ser nula");
        }
        if(request.getFechaReserva() == null){
            throw new Exception("La fecha de reserva no puede ser nula");
        }
        if(request.getEstado() == null || request.getEstado().trim().isEmpty()){
            throw new Exception("El estado no puede ser nulo o vacío");
        }
        if(!java.util.List.of("pendiente", "confirmado", "cancelado").contains(request.getEstado().toLowerCase())){
            throw new Exception("Estado inválido. Debe ser: pendiente, confirmado o cancelado");
        }
        if(request.getSocio() == null || request.getSocio() <= 0){
            throw new Exception("El socio no puede ser nulo");
        }
        if(request.getClase() == null || request.getClase() <= 0){
            throw new Exception("La clase no puede ser nula");
        }
    }
}