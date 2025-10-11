// Función para abrir el modal de nuevo usuario
function openModalUsuario() {
    console.log('Abriendo modal de nuevo usuario');
    document.getElementById('usuarioModal').style.display = 'block';
    document.getElementById('modalTitle').textContent = 'Nuevo Usuario';
    document.getElementById('usuarioForm').reset();
    document.getElementById('usuarioId').value = '';
    document.getElementById('errorMsg').style.display = 'none';

    // IMPORTANTE: Cambiar el action del formulario para crear
    document.getElementById('usuarioForm').action = '/admin/usuario/guardar';

    // Cambiar los nombres de los campos de contraseña para que coincidan con el endpoint de guardar
    document.getElementById('usuarioPwd').name = 'pwd';
    document.getElementById('usuarioPwd2').name = 'pwd2';

    // Mostrar y hacer obligatorios los campos de contraseña
    document.getElementById('passwordFields').style.display = 'block';
    document.getElementById('passwordFields2').style.display = 'block';
    document.getElementById('usuarioPwd').required = true;
    document.getElementById('usuarioPwd2').required = true;
    document.getElementById('usuarioPwd').placeholder = 'Mínimo 8 caracteres';
    document.getElementById('usuarioPwd2').placeholder = 'Confirme la contraseña';
}

// Función para cerrar el modal
function closeModalUsuario() {
    console.log('Cerrando modal');
    document.getElementById('usuarioModal').style.display = 'none';
}

// Función para editar usuario
function editarUsuario(id, usuarioNombre, correo, permiso) {
    console.log('Editando usuario:', id, usuarioNombre, correo, permiso);
    document.getElementById('usuarioModal').style.display = 'block';
    document.getElementById('modalTitle').textContent = 'Editar Usuario';
    document.getElementById('usuarioId').value = id;
    document.getElementById('usuarioNombre').value = usuarioNombre;
    document.getElementById('usuarioCorreo').value = correo;
    document.getElementById('usuarioPermiso').value = permiso;
    document.getElementById('errorMsg').style.display = 'none';

    // IMPORTANTE: Cambiar el action del formulario para editar
    document.getElementById('usuarioForm').action = '/admin/usuario/editar';

    // Ocultar completamente los campos de contraseña (ni siquiera se enviarán)
    document.getElementById('passwordFields').style.display = 'none';
    document.getElementById('passwordFields2').style.display = 'none';
    document.getElementById('usuarioPwd').required = false;
    document.getElementById('usuarioPwd2').required = false;
    document.getElementById('usuarioPwd').value = '';
    document.getElementById('usuarioPwd2').value = '';
}

// Función para eliminar usuario
function eliminarUsuario(id, nombre) {
    console.log('Intentando eliminar usuario:', id, nombre);
    if (confirm('¿Estás seguro de que deseas eliminar al usuario "' + nombre + '"?')) {
        window.location.href = '/admin/usuario/eliminar/' + id;
    }
}

// Funciones para manejar los botones de editar y eliminar usando data-attributes
function editarUsuarioBtn(button) {
    const id = button.getAttribute('data-id');
    const usuarioNombre = button.getAttribute('data-usuario');
    const correo = button.getAttribute('data-correo');
    const permiso = button.getAttribute('data-permiso');
    editarUsuario(id, usuarioNombre, correo, permiso);
}

function eliminarUsuarioBtn(button) {
    const id = button.getAttribute('data-id');
    const nombre = button.getAttribute('data-usuario');
    eliminarUsuario(id, nombre);
}

// Cerrar modal al hacer clic fuera de él
window.onclick = function(event) {
    const modal = document.getElementById('usuarioModal');
    if (event.target == modal) {
        closeModalUsuario();
    }
}

// Validar que las contraseñas coincidan
document.getElementById('usuarioForm').addEventListener('submit', function(e) {
    const pwd = document.getElementById('usuarioPwd').value;
    const pwd2 = document.getElementById('usuarioPwd2').value;
    const errorMsg = document.getElementById('errorMsg');

    // Solo validar si se está ingresando una contraseña
    if (pwd || pwd2) {
        if (pwd !== pwd2) {
            e.preventDefault();
            errorMsg.textContent = 'Las contraseñas no coinciden';
            errorMsg.style.display = 'block';
            return false;
        }

        if (pwd.length < 8) {
            e.preventDefault();
            errorMsg.textContent = 'La contraseña debe tener al menos 8 caracteres';
            errorMsg.style.display = 'block';
            return false;
        }
    }

    errorMsg.style.display = 'none';
    return true;
});

// Función para filtrar la tabla
function filterTable() {
    const input = document.getElementById('searchInput');
    const filter = input.value.toLowerCase();
    const table = document.querySelector('table tbody');
    const rows = table.getElementsByTagName('tr');

    for (let i = 0; i < rows.length; i++) {
        const cells = rows[i].getElementsByTagName('td');
        let found = false;

        for (let j = 0; j < cells.length; j++) {
            const cell = cells[j];
            if (cell) {
                const txtValue = cell.textContent || cell.innerText;
                if (txtValue.toLowerCase().indexOf(filter) > -1) {
                    found = true;
                    break;
                }
            }
        }

        rows[i].style.display = found ? '' : 'none';
    }
}

// Función para cerrar alertas
function closeAlert(button) {
    const alert = button.closest('.alert');
    if (alert) {
        alert.style.opacity = '0';
        alert.style.transition = 'opacity 0.3s ease';
        setTimeout(() => {
            alert.style.display = 'none';
        }, 300);
    }
}

console.log('Script de usuarios cargado correctamente');