function openModal() {
    document.getElementById("partidaModal").style.display = "block"
}

function closeModal() {
    document.getElementById("partidaModal").style.display = "none"
    document.getElementById("partidaForm").reset()
    //Eliminar documentos seleccionados
    const archivosOrigen = document.getElementById("archivosOrigen");
    let nDocumentos = archivosOrigen.children.length;
    while(nDocumentos >= 1) {
        archivosOrigen.removeChild(archivosOrigen.lastChild);
        nDocumentos -= 1;
    }
    //resetVistaPrevia();
}

function addMovimiento() {
    const container = document.getElementById("movimientos")
    const firstRow = container.querySelector(".movimiento-row")
    const newRow = firstRow.cloneNode(true)
    const rowCount = container.querySelectorAll(".movimiento-row").length + 1;
    newRow.querySelector(".cuenta-select").id = "cuentas" + rowCount;
    newRow.querySelector("datalist").id = "cuentas"+ rowCount;
    newRow.querySelectorAll("input, select").forEach((input) => (input.value = ""))
    container.appendChild(newRow)
    attachCalculateListeners()
}

function removeMovimiento(btn) {
    const container = document.getElementById("movimientos")
    if (container.querySelectorAll(".movimiento-row").length > 2) {
        btn.closest(".movimiento-row").remove()
        calculateTotals()
    }
}

function addDocumento(){
    const archivosOrigen = document.getElementById("archivosOrigen");
    const nDocumentos = archivosOrigen.children.length + 1;
    const nuevoDocumento = document.createElement("div");

    nuevoDocumento.style.cssText = "margin: 2rem;";
    nuevoDocumento.innerHTML = `
    <p>Documento #${nDocumentos}</p>
    <div class="form-group">
        <label for="nombreArchivo${nDocumentos}">Nombre del documento:</label>
        <input type="text" name="nombreArchivo${nDocumentos}" id="nombreArchivo${nDocumentos}" placeholder="Nombre para el documento ${nDocumentos}..." class="nombre-documento" required>
    </div>
    <label for="archivoOrigen${nDocumentos}" id="labelArchivo${nDocumentos}" class="btn btn-primary file-label"><i class="fa fa-file-arrow-up"></i> Subir</label>
        <input type="file" name="archivoOrigen${nDocumentos}" id="archivoOrigen${nDocumentos}" accept=".pdf,image/*" class="file-input archivo-origen" required>
    <div class="preview-container" id="previewContainer${nDocumentos}">
        <p>Ningún archivo seleccionado</p>
    </div>`;
    archivosOrigen.appendChild(nuevoDocumento);

    const inputArchivo = nuevoDocumento.querySelector(`#archivoOrigen${nDocumentos}`);
    inputArchivo.addEventListener("change", () => {
        cargarVistaPrevia(nDocumentos);
    });

    document.getElementById("eliminarUltimoDocumento").removeAttribute("disabled");
}

function removeDocumento(){
    const archivosOrigen = document.getElementById("archivosOrigen");

    archivosOrigen.removeChild(archivosOrigen.lastChild);
    const nDocumentos = archivosOrigen.children.length;
    if (nDocumentos < 1) {
        document.getElementById("eliminarUltimoDocumento").setAttribute("disabled", "");
    }
}

function calculateTotals() {
    let totalDebe = 0
    let totalHaber = 0

    document.querySelectorAll(".movimiento-row").forEach((row) => {
        const monto = Number.parseFloat(row.querySelector(".monto-input").value) || 0
        const tipo = row.querySelector(".tipo-select").value

        if (tipo === "D") {
            totalDebe += monto
        } else if (tipo === "H") {
            totalHaber += monto
        }
    })

    document.getElementById("totalDebe").textContent = "$" + totalDebe.toFixed(2)
    document.getElementById("totalHaber").textContent = "$" + totalHaber.toFixed(2)
    document.getElementById("diferencia").textContent = "$" + Math.abs(totalDebe - totalHaber).toFixed(2)
}

function attachCalculateListeners() {
    document.querySelectorAll(".monto-input, .tipo-select").forEach((element) => {
        element.removeEventListener("input", calculateTotals)
        element.addEventListener("input", calculateTotals)
    })
}

attachCalculateListeners()

function cargarVistaPrevia(previewId) {
    const input = document.getElementById('archivoOrigen'+previewId);
    const preview = document.getElementById('previewContainer'+previewId);
    const label = document.getElementById('labelArchivo'+previewId);
    const file = input.files[0];
    preview.innerHTML = '';

    if (!file) {
        preview.innerHTML = '<p>Ningún archivo seleccionado</p>';
        label.classList.remove('btn-secondary');
        label.classList.add('btn-primary');
        label.innerHTML = `<i class="fa fa-file-arrow-up"></i> Subir`;
        return;
    }

    const fileType = file.type;

    if (fileType.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = e => {
            preview.innerHTML = `<img src="${e.target.result}" alt="Vista previa">`;
        };
        reader.readAsDataURL(file);
    } else if (fileType === 'application/pdf') {
        // Usar blob URL para abrir PDF con el visor del navegador
        const fileURL = URL.createObjectURL(file);
        preview.innerHTML = `<iframe src="${fileURL}" width="100%" height="500" style="border:none;"></iframe>`;
    } else {
        preview.innerHTML = `<p>Tipo de archivo no soportado: ${file.name}</p>`;
    }

    label.classList.remove('btn-primary');
    label.classList.add('btn','btn-secondary');
    label.innerHTML = `<i class="fa fa-file-invoice"></i> ${file.name}`;

    const nombreArchivo = document.getElementById('nombreArchivo'+previewId);
    nombreArchivo.value = file.name.replace(/\.[^/.]+$/, "");
}

function resetVistaPrevia() {
    const previews = document.querySelectorAll(".preview-container");
    previews.forEach(preview => {
        preview.innerHTML = '<p>Ningún archivo seleccionado</p>';
    });
    const labels = document.querySelectorAll('.file-label');
    labels.forEach(label => {
       label.classList.remove('btn-secondary');
       label.classList.add('btn-primary');
       label.innerHTML = `<i class="fa fa-file-arrow-up"></i> Subir`;
    });
}

document.getElementById("partidaForm").addEventListener("submit", async (e) => {
    e.preventDefault()

    const concepto = document.getElementById("concepto").value
    const fechaPartida = document.getElementById("fechaPartida").value
    const movimientos = []

    const nombreDocumentosInputs = document.querySelectorAll(".nombre-documento");
    const archivosOrigenInputs = document.querySelectorAll(".archivo-origen");
    const montosArchivoInputs = document.querySelectorAll(".monto-archivo");

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

    for (const row of document.querySelectorAll(".movimiento-row")) {
        const listId = row.querySelector(".cuenta-select").getAttribute("list");
        const dataList = document.getElementById(listId);
        const cuentaInput = row.querySelector(".cuenta-select");
        const selectedOption = Array.from(dataList.options).find(
            option => option.value === cuentaInput.value
        );
        if(!selectedOption) {
            let mensajeDetallado = `La cuenta ingresada "${cuentaInput.value}" no es válida. Por favor, seleccione una cuenta de la lista sugerida.\n\nAsegúrese de elegir una opción existente para evitar errores en la creación de la partida.`;
            await Swal.fire({
                icon: 'warning',
                title: 'No selecciono correcatemente la cuenta',
                html: mensajeDetallado.replace(/\n/g, '<br>'),
                showCancelButton: true,
                showConfirmButton: false,
                cancelButtonText: 'Ok',
                cancelButtonColor: '#d63030'
            });
            continue; // Omitir si no hay opción seleccionada válida
        }
        const idCuenta = selectedOption.dataset.id;

        const monto = row.querySelector(".monto-input").value
        const tipo = row.querySelector(".tipo-select").value

        if (idCuenta && monto && tipo) {
            movimientos.push({ idCuenta, monto, tipo })
        }
    }

    // Función auxiliar para crear el FormData
    const crearFormData = (incluirForzar = false) => {
        const fd = new FormData();
        fd.append('concepto', concepto);
        fd.append('fechaPartida', fechaPartida);
        fd.append('movimientos', JSON.stringify(movimientos));
        fd.append("nombresArchivos", JSON.stringify(nombreDocumentosArray));

        archivosOrigenArray.forEach((archivo) => {
            fd.append('archivosOrigen', archivo);
        });

        if (incluirForzar) {
            fd.append('forzar', 'true');
        }

        return fd;
    };

    const formData = crearFormData();

    try {
        const response = await fetch("/partidas/crear", {
            method: "POST",
            body: formData
        })

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

                    const responseForzado = await fetch("/partidas/crear", {
                        method: "POST",
                        body: formDataForzado
                    });

                    const dataForzado = await responseForzado.json();

                    if (responseForzado.ok && dataForzado.success) {
                        // Solo cerramos si es exitoso
                        closeModal();
                        await alerta("Partida creada exitosamente", 'success');
                        location.reload();
                    } else {
                        // Error al forzar: NO cerramos la modal
                        await alerta(dataForzado.error || "Error al crear la partida", 'error');
                        // La modal permanece abierta
                    }
                }
                // Si cancela, no hacemos nada - la modal sigue abierta
            } else if (data.success) {
                // Éxito normal sin advertencias
                closeModal();
                await alerta("Partida creada exitosamente", 'success');
                location.reload();
            }
        } else {
            // Error en la respuesta: NO cerramos la modal
            await alerta(data.error || "Error al crear la partida", 'error');
            // La modal permanece abierta con los datos
        }
    } catch (error) {
        // Error de red/excepción: NO cerramos la modal
        await alerta("Error al crear la partida: " + error.message, 'error');
        // La modal permanece abierta con los datos
    }
});


async function alerta(message, type = 'info', title = 'OneDi system') {
    return await Swal.fire({
        icon: type,
        title: title,
        text: message,
        confirmButtonText: 'Aceptar'
    });
}

// Detectar si se debe abrir la modal automáticamente desde el sidebar
document.addEventListener('DOMContentLoaded', function() {
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('openModal') === 'true') {
        openModal();
        // Limpiar el parámetro de la URL sin recargar la página
        window.history.replaceState({}, document.title, window.location.pathname);
    }
});

