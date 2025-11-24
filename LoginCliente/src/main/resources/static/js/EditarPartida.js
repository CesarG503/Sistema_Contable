let editPartidaIdGlobal = null;
let editCuentasData = [];
let editDocumentosAEliminar = [];

async function openEditModal(button) {
    const partidaId = button.getAttribute('data-partida-id');
    editPartidaIdGlobal = partidaId;
    editDocumentosAEliminar = [];

    try {
        // Obtener datos de la partida
        const response = await fetch(`/partidas/${partidaId}`);
        if (!response.ok) {
            throw new Error('Error al cargar los datos de la partida');
        }

        const data = await response.json();

        // Llenar el formulario
        document.getElementById('editPartidaId').value = partidaId;
        document.getElementById('editFechaPartida').value = data.fecha;
        document.getElementById('editConcepto').value = data.concepto;

        // Cargar cuentas disponibles
        await loadEditCuentas();

        // Cargar movimientos
        loadEditMovimientos(data.movimientos);

        // Cargar documentos existentes
        loadEditDocumentos(data.documentos);

        // Limpiar nuevos archivos
        document.getElementById('editArchivosOrigen').innerHTML = '';
        document.getElementById('eliminarUltimoEditDocumento').setAttribute('disabled', '');

        // Mostrar modal
        document.getElementById('editarPartidaModal').style.display = 'block';

        // Calcular totales iniciales
        calculateEditTotals();
    } catch (error) {
        await alerta('Error al cargar los datos: ' + error.message, 'error');
    }
}

function closeEditModal() {
    document.getElementById('editarPartidaModal').style.display = 'none';
    document.getElementById('editarPartidaForm').reset();
    editPartidaIdGlobal = null;
    editDocumentosAEliminar = [];
}

async function loadEditCuentas() {
    try {
        const response = await fetch('/cuentas/todas');
        if (!response.ok) {
            throw new Error('Error al cargar cuentas');
        }
        editCuentasData = await response.json();
    } catch (error) {
        console.error('Error cargando cuentas:', error);
        editCuentasData = [];
    }
}

function loadEditMovimientos(movimientos) {
    const container = document.getElementById('editMovimientos');
    container.innerHTML = '';

    movimientos.forEach((mov, index) => {
        const row = createEditMovimientoRow(mov, index);
        container.appendChild(row);
    });

    attachEditCalculateListeners();
}

function createEditMovimientoRow(movimiento = null, index = 0) {
    const div = document.createElement('div');
    div.className = 'movimiento-row';

    const datalistId = `editCuentas${index}`;
    const cuentaNombre = movimiento ? getCuentaNombreById(movimiento.idCuenta) : '';

    div.innerHTML = `
        <div class="form-group">
            <label>Cuenta:</label>
            <input list="${datalistId}" 
                   type="text" 
                   class="cuenta-select" 
                   value="${cuentaNombre}"
                   required>
            <datalist id="${datalistId}">
                ${editCuentasData.map(cuenta => 
                    `<option value="${cuenta.nombre}" data-id="${cuenta.idCuenta}"></option>`
                ).join('')}
            </datalist>
        </div>
        <div class="form-group">
            <label>Monto:</label>
            <input type="number" 
                   class="monto-input" 
                   step="0.01" 
                   min="0" 
                   value="${movimiento ? movimiento.monto : ''}"
                   required>
        </div>
        <div class="form-group">
            <label>Tipo:</label>
            <select class="tipo-select" required>
                <option value="D" ${movimiento && movimiento.tipo === 'D' ? 'selected' : ''}>Debe</option>
                <option value="H" ${movimiento && movimiento.tipo === 'H' ? 'selected' : ''}>Haber</option>
            </select>
        </div>
        <button type="button" class="btn btn-remove" onclick="removeEditMovimiento(this)">X</button>
    `;

    return div;
}

function getCuentaNombreById(idCuenta) {
    const cuenta = editCuentasData.find(c => c.idCuenta === idCuenta);
    return cuenta ? cuenta.nombre : '';
}

function addEditMovimiento() {
    const container = document.getElementById('editMovimientos');
    const rowCount = container.querySelectorAll('.movimiento-row').length;
    const newRow = createEditMovimientoRow(null, rowCount);
    container.appendChild(newRow);
    attachEditCalculateListeners();
}

async function removeEditMovimiento(btn) {
    const container = document.getElementById('editMovimientos');
    if (container.querySelectorAll('.movimiento-row').length > 1) {
        btn.closest('.movimiento-row').remove();
        calculateEditTotals();
    } else {
        await alerta('Debe haber al menos un movimiento', 'warning');
    }
}

function loadEditDocumentos(documentos) {
    const container = document.getElementById('editDocumentosExistentes');
    container.innerHTML = '';

    if (!documentos || documentos.length === 0) {
        container.innerHTML = '<p style="color: #666;">No hay documentos asociados</p>';
        return;
    }

    documentos.forEach(doc => {
        const docDiv = document.createElement('div');
        docDiv.className = 'documento-existente';
        docDiv.style.cssText = 'border: 1px solid #ddd; padding: 10px; margin: 10px 0; border-radius: 5px; display: flex; justify-content: space-between; align-items: center;';
        docDiv.innerHTML = `
            <div>
                <p>name: <strong>${doc.nombre}</strong></p>
                <a href="/${doc.ruta}" target="_blank" class="btn btn-sm">Ver documento</a>
            </div>
            <button type="button" 
                    class="btn btn-delete" 
                    onclick="marcarDocumentoParaEliminar(${doc.id}, this)"
                    title="Eliminar documento">
                <i class="fa-solid fa-trash"></i>
            </button>
        `;
        container.appendChild(docDiv);
    });
}

async function marcarDocumentoParaEliminar(docId, button) {
    if (confirm('¿Estás seguro de eliminar este documento?')) {
        editDocumentosAEliminar.push(docId);
        button.closest('.documento-existente').style.opacity = '0.5';
        button.closest('.documento-existente').style.textDecoration = 'line-through';
        button.disabled = true;
        await alerta('Documento marcado para eliminar. Se eliminará al guardar.', 'info');
    }
}

function addEditDocumento() {
    const container = document.getElementById('editArchivosOrigen');
    const nDocumentos = container.children.length + 1;
    const nuevoDocumento = document.createElement('div');

    nuevoDocumento.style.cssText = 'margin: 2rem; border: 1px solid #ddd; padding: 15px; border-radius: 5px;';
    nuevoDocumento.innerHTML = `
        <p><strong>Nuevo Documento #${nDocumentos}</strong></p>
        <div class="form-group">
            <label for="editNombreArchivo${nDocumentos}">Nombre del documento:</label>
            <input type="text" 
                   id="editNombreArchivo${nDocumentos}" 
                   placeholder="Nombre para el documento ${nDocumentos}..." 
                   class="edit-nombre-documento" 
                   required>
        </div>
        <label for="editArchivoOrigen${nDocumentos}" class="file-label">Subir archivo</label>
        <input type="file" 
               id="editArchivoOrigen${nDocumentos}" 
               accept=".pdf,image/*" 
               class="file-input edit-archivo-origen" 
               required>
        <div class="preview-container" id="editPreviewContainer${nDocumentos}">
            <p>Ningún archivo seleccionado</p>
        </div>

    `;
    container.appendChild(nuevoDocumento);

    const inputArchivo = nuevoDocumento.querySelector(`#editArchivoOrigen${nDocumentos}`);
    inputArchivo.addEventListener('change', () => {
        cargarEditVistaPrevia(nDocumentos);
    });

    document.getElementById('eliminarUltimoEditDocumento').removeAttribute('disabled');
}

function removeEditDocumento() {
    const container = document.getElementById('editArchivosOrigen');
    if (container.children.length > 0) {
        container.removeChild(container.lastChild);
        if (container.children.length === 0) {
            document.getElementById('eliminarUltimoEditDocumento').setAttribute('disabled', '');
        }
    }
}

function cargarEditVistaPrevia(previewId) {
    const input = document.getElementById('editArchivoOrigen' + previewId);
    const preview = document.getElementById('editPreviewContainer' + previewId);
    const file = input.files[0];
    preview.innerHTML = '';

    if (!file) {
        preview.innerHTML = '<p>Ningún archivo seleccionado</p>';
        return;
    }

    const fileType = file.type;

    if (fileType.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = e => {
            preview.innerHTML = `<img src="${e.target.result}" alt="Vista previa" style="max-width: 100%; height: auto;">`;
        };
        reader.readAsDataURL(file);
    } else if (fileType === 'application/pdf') {
        const fileURL = URL.createObjectURL(file);
        preview.innerHTML = `<iframe src="${fileURL}" width="100%" height="500" style="border:none;"></iframe>`;
    } else {
        preview.innerHTML = `<p>Tipo de archivo no soportado: ${file.name}</p>`;
    }
}

function calculateEditTotals() {
    let totalDebe = 0;
    let totalHaber = 0;

    document.querySelectorAll('#editMovimientos .movimiento-row').forEach((row) => {
        const monto = parseFloat(row.querySelector('.monto-input').value) || 0;
        const tipo = row.querySelector('.tipo-select').value;

        if (tipo === 'D') {
            totalDebe += monto;
        } else if (tipo === 'H') {
            totalHaber += monto;
        }
    });

    document.getElementById('editTotalDebe').textContent = '$' + totalDebe.toFixed(2);
    document.getElementById('editTotalHaber').textContent = '$' + totalHaber.toFixed(2);
    document.getElementById('editDiferencia').textContent = '$' + Math.abs(totalDebe - totalHaber).toFixed(2);
}

function attachEditCalculateListeners() {
    document.querySelectorAll('#editMovimientos .monto-input, #editMovimientos .tipo-select').forEach((element) => {
        element.removeEventListener('input', calculateEditTotals);
        element.addEventListener('input', calculateEditTotals);
    });
}

// Submit del formulario de edición
const editForm = document.getElementById('editarPartidaForm');
if (!editForm) {
    console.error('No se encontró el formulario editarPartidaForm');
} else {
    console.log('Formulario de edición encontrado, agregando listener');
    editForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const partidaId = document.getElementById('editPartidaId').value;
        const concepto = document.getElementById('editConcepto').value;
        const fechaPartida = document.getElementById('editFechaPartida').value;

        // Validar que los totales coincidan
        const totalDebeText = document.getElementById('editTotalDebe').textContent.replace('$', '');
        const totalHaberText = document.getElementById('editTotalHaber').textContent.replace('$', '');
        const totalDebe = parseFloat(totalDebeText);
        const totalHaber = parseFloat(totalHaberText);

        if (Math.abs(totalDebe - totalHaber) > 0.01) {
            await alerta('El total del Debe debe ser igual al total del Haber', 'error');
            return;
        }

        // Recopilar movimientos
    const movimientos = [];
    document.querySelectorAll('#editMovimientos .movimiento-row').forEach((row) => {
        const cuentaInput = row.querySelector('.cuenta-select');
        const datalistId = cuentaInput.getAttribute('list');
        const dataList = document.getElementById(datalistId);
        const selectedOption = Array.from(dataList.options).find(
            option => option.value === cuentaInput.value
        );

        if (!selectedOption) {
            return;
        }

        const idCuenta = selectedOption.dataset.id;
        const monto = row.querySelector('.monto-input').value;
        const tipo = row.querySelector('.tipo-select').value;

        if (idCuenta && monto && tipo) {
            movimientos.push({ idCuenta, monto, tipo });
        }
    });

    if (movimientos.length === 0) {
        await alerta('Debe agregar al menos un movimiento', 'error');
        return;
    }

    // Recopilar nuevos documentos
    const nombreDocumentosInputs = document.querySelectorAll('.edit-nombre-documento');
    const archivosOrigenInputs = document.querySelectorAll('.edit-archivo-origen');
    const montosArchivoInputs = document.querySelectorAll('.edit-monto-archivo');

    let nombreDocumentosArray = [];
    let archivosOrigenArray = [];
    let montosArchivoArray = [];

    nombreDocumentosInputs.forEach((input) => {
        if (input.value) {
            nombreDocumentosArray.push(input.value);
        }
    });

    archivosOrigenInputs.forEach((input) => {
        if (input.files[0]) {
            archivosOrigenArray.push(input.files[0]);
        }
    });

    montosArchivoInputs.forEach((input) => {
        if (input.value) {
            montosArchivoArray.push(input.value);
        }
    });

    // Función auxiliar para crear el FormData
    const crearFormData = (incluirForzar = false) => {
        const fd = new FormData();
        fd.append('concepto', concepto);
        fd.append('fechaPartida', fechaPartida);
        fd.append('movimientos', JSON.stringify(movimientos));
        fd.append('nombresArchivos', JSON.stringify(nombreDocumentosArray));
        fd.append('montosArchivo', JSON.stringify(montosArchivoArray));
        fd.append('documentosAEliminar', JSON.stringify(editDocumentosAEliminar));

        archivosOrigenArray.forEach((archivo) => {
            fd.append('archivosOrigen', archivo);
        });

        if (incluirForzar) {
            fd.append('forzar', 'true');
        }

        return fd;
    };

    // Crear FormData
    const formData = crearFormData();

    try {
        const response = await fetch(`/partidas/actualizar/${partidaId}`, {
            method: 'POST',
            body: formData
        });

        const data = await response.json();

        if (response.ok) {
            // Verificar si hay advertencia de cuentas negativas
            if (data.warning) {
                // No cerramos la modal para conservar los datos ingresados

                // Construir mensaje detallado
                let mensajeDetallado = data.mensaje + "\n\n";

                const result = await Swal.fire({
                    icon: 'warning',
                    title: 'Advertencia de Saldo Negativo',
                    html: mensajeDetallado.replace(/\n/g, '<br>'),
                    showCancelButton: true,
                    confirmButtonText: 'Sí, continuar',
                    cancelButtonText: 'Cancelar',
                    confirmButtonColor: '#d33',
                    cancelButtonColor: '#3085d6'
                });

                if (result.isConfirmed) {
                    // Usuario confirmó, crear nuevo FormData con forzar=true
                    const formDataForzado = crearFormData(true);

                    const responseForzado = await fetch(`/partidas/actualizar/${partidaId}`, {
                        method: 'POST',
                        body: formDataForzado
                    });

                    const dataForzado = await responseForzado.json();

                    if (responseForzado.ok && dataForzado.success) {
                        closeEditModal();
                        await alerta('Partida actualizada exitosamente', 'success');
                        location.reload();
                    } else {
                        await alerta('Error al actualizar la partida: ' + (dataForzado.error || 'Error desconocido'), 'error');
                    }
                } else {
                    // Usuario canceló: no hacemos nada, la modal sigue abierta con los datos intactos
                }
            } else if (data.success) {
                // Éxito normal sin advertencias
                closeEditModal();
                await alerta('Partida actualizada exitosamente', 'success');
                location.reload();
            }
        } else {
            await alerta('Error al actualizar la partida: ' + (data.error || 'Error desconocido'), 'error');
        }
    } catch (error) {
        console.error('Error en catch:', error);
        await alerta('Error al actualizar la partida: ' + error.message, 'error');
    }
    });
}
async function alerta(message, type = 'info', title = 'OneDi system') {
    console.log('Alerta llamada:', message, type, title);

    if (typeof Swal === 'undefined') {
        console.error('SweetAlert2 no está cargado - NO se mostrará ninguna alerta');
        return Promise.resolve(); // No mostrar nada si SweetAlert no está cargado
    }

    // Temporalmente ocultar el modal de edición si está visible
    const editModal = document.getElementById('editarPartidaModal');
    const wasVisible = editModal && editModal.style.display === 'block';

    if (wasVisible) {
        console.log('Ocultando modal temporalmente para mostrar alerta');
        editModal.style.display = 'none';
    }

    const result = await Swal.fire({
        icon: type,
        title: title,
        text: message,
        confirmButtonText: 'Aceptar'
    });

    // Restaurar visibilidad del modal
    if (wasVisible) {
        console.log('Restaurando modal');
        editModal.style.display = 'block';
    }

    return result;
}

// Hacer la función global
window.alerta = alerta;
console.log('EditarPartida.js: función alerta definida');

// Cerrar modal al hacer clic fuera
window.addEventListener('click', (event) => {
    const modal = document.getElementById('editarPartidaModal');
    if (event.target === modal) {
        closeEditModal();
    }
});
