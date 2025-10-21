package cl.gym.gimnasio.service.impl;

import cl.gym.gimnasio.dto.UsuarioDTO;
import cl.gym.gimnasio.dto.request.CreateUsuarioRequest;
import cl.gym.gimnasio.dto.response.CreateUsuarioResponse;
import cl.gym.gimnasio.mapper.UsuarioMapper;
import cl.gym.gimnasio.model.Usuario;
import cl.gym.gimnasio.repository.UsuarioRepository;
import cl.gym.gimnasio.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> getAllUsuarios() {
        return usuarioRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO getUsuarioPorId(Integer id) {
        // CAMBIADO: findById en lugar de getReferenceById
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        return UsuarioMapper.modelToDTO(usuario);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateUsuarioResponse createUsuario(CreateUsuarioRequest createUsuarioRequest) throws Exception {
        System.out.println("🟡 Iniciando creación de usuario: " + createUsuarioRequest.getEmail());

        // Validaciones
        validarUsuarioRequest(createUsuarioRequest);

        // Verificar si el email ya existe
        if (usuarioRepository.existsByEmail(createUsuarioRequest.getEmail())) {
            throw new Exception("El email ya está registrado.");
        }

        try {
            // Convertir de Request a Model
            Usuario usuario = UsuarioMapper.createRequestToModel(createUsuarioRequest);

            // HASHEAR LA CONTRASEÑA
            String hashedPassword = passwordEncoder.encode(createUsuarioRequest.getPassword());
            usuario.setPassword(hashedPassword);
            usuario.setFechaRegistro(new Date());

            System.out.println("🟡 Guardando usuario en BD...");

            // Persistir
            Usuario usuarioGuardado = usuarioRepository.save(usuario);
            usuarioRepository.flush(); // Forzar flush para detectar errores

            System.out.println("✅ Usuario guardado exitosamente. ID: " + usuarioGuardado.getIdUsuario());

            // Convertir a response
            CreateUsuarioResponse response = UsuarioMapper.modelToCreateResponse(usuarioGuardado);
            System.out.println("✅ Respuesta preparada para: " + response.getEmail());

            return response;

        } catch (Exception e) {
            System.err.println("❌ ERROR CRÍTICO al guardar usuario: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("Error interno del servidor al crear usuario: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public CreateUsuarioResponse updateUsuario(Integer id, CreateUsuarioRequest createUsuarioRequest) throws Exception {
        System.out.println("🟡 Actualizando usuario ID: " + id);

        // Validar que el usuario exista
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new Exception("El usuario no fue encontrado."));

        // Validaciones
        validarUsuarioRequest(createUsuarioRequest);

        // Verificar si el email ya existe en otro usuario
        if (!usuario.getEmail().equals(createUsuarioRequest.getEmail()) &&
                usuarioRepository.existsByEmail(createUsuarioRequest.getEmail())) {
            throw new Exception("El email ya está registrado en otro usuario.");
        }

        // Actualizar los datos del usuario
        usuario.setNombre(createUsuarioRequest.getNombre());
        usuario.setEmail(createUsuarioRequest.getEmail());
        usuario.setTelefono(createUsuarioRequest.getTelefono());
        usuario.setRol(createUsuarioRequest.getRol());

        // ACTUALIZAR CONTRASEÑA SOLO SI SE PROPORCIONA UNA NUEVA
        if (createUsuarioRequest.getPassword() != null &&
                !createUsuarioRequest.getPassword().isBlank()) {
            String hashedPassword = passwordEncoder.encode(createUsuarioRequest.getPassword());
            usuario.setPassword(hashedPassword);
        }

        // Guardar Usuario actualizado
        Usuario usuarioActualizado = usuarioRepository.save(usuario);
        usuarioRepository.flush();

        System.out.println("✅ Usuario actualizado exitosamente. ID: " + usuarioActualizado.getIdUsuario());

        return UsuarioMapper.modelToCreateResponse(usuarioActualizado);
    }

    @Override
    @Transactional
    public void deleteUsuario(Integer id) throws Exception {
        System.out.println("🟡 Eliminando usuario ID: " + id);

        // Verificamos que exista el usuario
        if (!usuarioRepository.existsById(id)) {
            throw new Exception("Usuario no encontrado");
        }

        // Eliminamos
        usuarioRepository.deleteById(id);
        System.out.println("✅ Usuario eliminado exitosamente. ID: " + id);
    }

    private void validarUsuarioRequest(CreateUsuarioRequest request) throws Exception {
        if (request == null) {
            throw new Exception("El usuario no puede ser nulo.");
        }
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new Exception("El nombre no puede ser nulo.");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new Exception("El email no puede ser nulo.");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new Exception("La password no puede ser nula.");
        }
        if (request.getTelefono() == null || request.getTelefono().isBlank()) {
            throw new Exception("El telefono no puede ser nulo.");
        }
        if (request.getRol() == null || request.getRol().isBlank()) {
            throw new Exception("El rol no puede ser nulo.");
        }
    }
}