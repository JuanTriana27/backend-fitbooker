package cl.gym.gimnasio.service.impl;

import cl.gym.gimnasio.dto.ClaseDTO;
import cl.gym.gimnasio.dto.request.CreateClaseRequest;
import cl.gym.gimnasio.dto.response.CreateClaseResponse;
import cl.gym.gimnasio.mapper.ClaseMapper;
import cl.gym.gimnasio.model.Clase;
import cl.gym.gimnasio.model.Usuario;
import cl.gym.gimnasio.repository.ClaseRepository;
import cl.gym.gimnasio.repository.UsuarioRepository;
import cl.gym.gimnasio.service.ClaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClaseServiceImpl implements ClaseService {

    private final ClaseRepository claseRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Clase> getAllClases(){
        return claseRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public ClaseDTO getClasePorId(Integer id){
        // CAMBIADO: findById en lugar de getReferenceById
        Clase clase = claseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Clase no encontrada con ID: " + id));
        return ClaseMapper.modelToDTO(clase);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateClaseResponse createClase(CreateClaseRequest createClaseRequest) throws Exception{
        System.out.println("🟡 Iniciando creación de clase: " + createClaseRequest.getNombre());

        // Validaciones
        validarClaseRequest(createClaseRequest);

        // Obtener entidad relacionada
        Usuario usuario = usuarioRepository.findById(createClaseRequest.getIdCoach())
                .orElseThrow(() -> new Exception("Coach no encontrado con ID: " + createClaseRequest.getIdCoach()));

        // Mapear request a modelo y establecer relaciones
        Clase clase = ClaseMapper.createRequestToModel(createClaseRequest);
        clase.setCoach(usuario);

        // Guardar en la db
        Clase claseGuardada = claseRepository.save(clase);
        claseRepository.flush();

        System.out.println("✅ Clase guardada exitosamente. ID: " + claseGuardada.getIdClase());

        return ClaseMapper.modelToResponse(claseGuardada);
    }

    @Override
    @Transactional
    public CreateClaseResponse updateClase(Integer id, CreateClaseRequest createClaseRequest) throws Exception {
        System.out.println("🟡 Actualizando clase ID: " + id);

        // Verificamos que exista
        Clase clase = claseRepository.findById(id)
                .orElseThrow(() -> new Exception("Clase no Encontrada"));

        // Validaciones
        validarClaseRequest(createClaseRequest);

        // Obtener entidad relacionada
        Usuario usuario = usuarioRepository.findById(createClaseRequest.getIdCoach())
                .orElseThrow(() -> new Exception("Coach no encontrado con ID: " + createClaseRequest.getIdCoach()));

        // Actualizar campos
        clase.setNombre(createClaseRequest.getNombre());
        clase.setDescripcion(createClaseRequest.getDescripcion());
        clase.setFechaClase(createClaseRequest.getFechaClase());
        clase.setHoraInicio(createClaseRequest.getHoraInicio());
        clase.setHoraFin(createClaseRequest.getHoraFin());
        clase.setCupoMaximo(createClaseRequest.getCupoMaximo());
        clase.setCupoDisponible(createClaseRequest.getCupoDisponible());
        clase.setCoach(usuario);

        // Guardar cambios
        Clase claseActualizada = claseRepository.save(clase);
        claseRepository.flush();

        System.out.println("✅ Clase actualizada exitosamente. ID: " + claseActualizada.getIdClase());

        return ClaseMapper.modelToResponse(claseActualizada);
    }

    @Override
    @Transactional
    public void deleteClase(Integer id) throws Exception{
        System.out.println("🟡 Eliminando clase ID: " + id);

        // Verificamos que exista la clase
        if(!claseRepository.existsById(id)){
            throw new Exception("La Clase no existe");
        }

        // Eliminamos
        claseRepository.deleteById(id);
        System.out.println("✅ Clase eliminada exitosamente. ID: " + id);
    }

    private void validarClaseRequest(CreateClaseRequest request) throws Exception {
        if(request == null){
            throw new Exception("La Clase no puede ser nula");
        }
        if(request.getNombre() == null || request.getNombre().isBlank()){
            throw new Exception("El nombre no puede ser nulo");
        }
        if(request.getDescripcion() == null || request.getDescripcion().isBlank()){
            throw new Exception("La descripcion no puede ser nula");
        }
        if(request.getFechaClase()== null){
            throw new Exception("La fecha no puede ser nula");
        }
        if(request.getHoraInicio() == null){
            throw new Exception("La hora de inicio no puede ser nula");
        }
        if(request.getHoraFin() == null){
            throw new Exception("La hora de fin no puede ser nula");
        }
        if(request.getCupoMaximo() == null){
            throw new Exception("El cupo máximo no puede ser nulo");
        }
        if(request.getCupoDisponible() == null){
            throw new Exception("El cupo disponible no puede ser nulo");
        }
        if (request.getIdCoach() == null || request.getIdCoach() <= 0){
            throw new Exception("El Coach no puede ser nulo");
        }
    }
}