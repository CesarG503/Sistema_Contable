// Mapeo de tipo de cuenta a naturaleza
const naturalezaMap = {
    'Activo': { value: 'D', display: 'Deudora' },
    'Pasivo': { value: 'A', display: 'Acreedora' },
    'Capital': { value: 'A', display: 'Acreedora' },
    'Ingreso': { value: 'A', display: 'Acreedora' },
    'Gasto': { value: 'D', display: 'Deudora' }
};

// Establecer la naturaleza según el tipo de cuenta
function setNaturaleza() {
    const tipo = document.getElementById('tipo').value;
    const naturalezaHidden = document.getElementById('naturaleza');
    const naturalezaDisplay = document.getElementById('naturaleza-display');

    if (naturalezaMap[tipo]) {
        naturalezaHidden.value = naturalezaMap[tipo].value;
        naturalezaDisplay.value = naturalezaMap[tipo].display;
    } else {
        naturalezaHidden.value = '';
        naturalezaDisplay.value = '';
    }
}

// Validar si hay cambio de naturaleza antes de enviar el formulario
function validarCambioNaturaleza(event) {
    const naturalezaOriginal = document.getElementById('naturalezaOriginal').value;
    const nuevaNaturaleza = document.getElementById('naturaleza').value;
    const confirmarCambio = document.getElementById('confirmarCambio').value;

    // Si la naturaleza cambió y no ha sido confirmado aún
    if (naturalezaOriginal !== nuevaNaturaleza && confirmarCambio === 'false') {
        event.preventDefault();

        Swal.fire({
            title: '¡Advertencia!',
            html: 'Estás cambiando la naturaleza de la cuenta de <strong>' +
                  (naturalezaOriginal === 'D' ? 'Deudora' : 'Acreedora') +
                  '</strong> a <strong>' +
                  (nuevaNaturaleza === 'D' ? 'Deudora' : 'Acreedora') +
                  '</strong>.<br><br>' +
                  'Esto afectará la ecuación contable y deberás hacer ajustes para mantener el balance de las cuentas.<br><br>' +
                  '<strong>¿Estás seguro de continuar?</strong>',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#d33',
            cancelButtonColor: '#3085d6',
            confirmButtonText: 'Sí, cambiar naturaleza',
            cancelButtonText: 'Cancelar',
            customClass: {
                popup: 'swal-wide'
            }
        }).then((result) => {
            if (result.isConfirmed) {
                document.getElementById('confirmarCambio').value = 'true';
                document.getElementById('formEditarCuenta').submit();
            }
        });

        return false;
    }

    return true;
}

// Inicializar la vista de naturaleza cuando carga la página
document.addEventListener('DOMContentLoaded', function() {
    setNaturaleza();
});

