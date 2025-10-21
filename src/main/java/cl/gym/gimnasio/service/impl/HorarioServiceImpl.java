package cl.gym.gimnasio.service.impl;

import cl.gym.gimnasio.dto.HorarioDTO;
import cl.gym.gimnasio.dto.request.CreateHorarioRequest;
import cl.gym.gimnasio.dto.response.CreateHorarioResponse;
import cl.gym.gimnasio.mapper.HorarioMapper;
import cl.gym.gimnasio.model.Horario;
import cl.gym.gimnasio.model.Usuario;
import cl.gym.gimnasio.repository.HorarioRepository;
import cl.gym.gimnasio.repository.UsuarioRepository;
import cl.gym.gimnasio.service.HorarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class HorarioServiceImpl implements HorarioService {

    private final HorarioRepository horarioRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Horario> getAllHorarios(){
        return horarioRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public HorarioDTO getHorarioPorId(Integer id) {
        // CAMBIADO: findById en lugar de getReferenceById
        Horario horario = horarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Horario no encontrado con ID: " + id));
        return HorarioMapper.modelToDTO(horario);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateHorarioResponse createHorario(CreateHorarioRequest createHorarioRequest) throws Exception {
        System.out.println("🟡 Iniciando creación de horario para coach: " + createHorarioRequest.getCoach());

        // Validaciones
        validarHorarioRequest(createHorarioRequest);

        // Obtener entidad relacionada
        Usuario usuario = usuarioRepository.findById(createHorarioRequest.getCoach())
                .orElseThrow(() -> new Exception("Coach no encontrado con ID: " + createHorarioRequest.getCoach()));

        // Mapear request a modelo
        Horario horario = HorarioMapper.createRequestToModel(createHorarioRequest);
        horario.setCoach(usuario);

        // Guardar en la db
        Horario horarioGuardado = horarioRepository.save(horario);
        horarioRepository.flush();

        System.out.println("✅ Horario guardado exitosamente. ID: " + horarioGuardado.getIdHorario());

        return HorarioMapper.modelToResponse(horarioGuardado);
    }

    @Override
    @Transactional
    public CreateHorarioResponse updateHorario(Integer id, CreateHorarioRequest createHorarioRequest) throws Exception {
        System.out.println("🟡 Actualizando horario ID: " + id);

        // Verificamos que exista
        Horario horario = horarioRepository.findById(id)
                .orElseThrow(() -> new Exception("Horario no encontrado"));

        // Validaciones
        validarHorarioRequest(createHorarioRequest);

        // Obtener entidad relacionada
        Usuario usuario = usuarioRepository.findById(createHorarioRequest.getCoach())
                .orElseThrow(() -> new Exception("Coach no encontrado con ID: " + createHorarioRequest.getCoach()));

        // Actualizar campos
        horario.setDiaSemana(createHorarioRequest.getDiaSemana());
        horario.setHoraInicio(createHorarioRequest.getHoraInicio());
        horario.setHoraFin(createHorarioRequest.getHoraFin());
        horario.setCoach(usuario);

        // Guardar cambios
        Horario horarioActualizado = horarioRepository.save(horario);
        horarioRepository.flush();

        System.out.println("✅ Horario actualizado exitosamente. ID: " + horarioActualizado.getIdHorario());

        return HorarioMapper.modelToResponse(horarioActualizado);
    }

    @Override
    @Transactional
    public void deleteHorario(Integer id) throws Exception {
        System.out.println("🟡 Eliminando horario ID: " + id);

        // Verificamos que exista el horario
        if(!horarioRepository.existsById(id)){
            throw new Exception("El horario no existe");
        }

        // Eliminamos
        horarioRepository.deleteById(id);
        System.out.println("✅ Horario eliminado exitosamente. ID: " + id);
    }

    private void validarHorarioRequest(CreateHorarioRequest request) throws Exception {
        if(request == null){
            throw new Exception("El horario no puede ser nulo");
        }
        if(request.getDiaSemana() == null || request.getDiaSemana().isBlank()){
            throw new Exception("El día de semana no puede ser nulo");
        }
        if(request.getHoraInicio() == null){
            throw new Exception("La hora de inicio no puede ser nula");
        }
        if(request.getHoraFin() == null){
            throw new Exception("La hora de fin no puede ser nula");
        }
        if(request.getCoach() == null || request.getCoach() <= 0){
            throw new Exception("El coach no puede ser nulo");
        }
    }
}