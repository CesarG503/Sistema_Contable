function openModal() {
    document.getElementById("partidaModal").style.display = "block"
}

function closeModal() {
    const modal = document.getElementById("partidaModal");
    if (modal) modal.style.display = "none";
    const form = document.getElementById("partidaForm");
    if (form) form.reset();
    // Eliminar documentos seleccionados (remover todos los hijos)
    const archivosOrigen = document.getElementById("archivosOrigen");
    if (archivosOrigen) {
        while (archivosOrigen.firstChild) {
            archivosOrigen.removeChild(archivosOrigen.lastChild);
        }
    }
    resetVistaPrevia();
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
    <label for="archivoOrigen${nDocumentos}" id="labelArchivo${nDocumentos}" class="btn btn-primary file-label"><i class="fa fa-file"></i> Subir</label>
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

    const label = document.getElementById('labelArchivo'+previewId);
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
       label.innerHTML = `<i class="fa fa-file"></i> Subir`;
    });
}

// proteger elemento que puede no existir inicialmente
const archivoOrigenElement = document.getElementById("archivoOrigen");
if (archivoOrigenElement) {
    archivoOrigenElement.addEventListener("change", () =>{
        cargarVistaPrevia('');
    });
}

// Obtener token CSRF (si está presente en la página)
function getCsrf() {
    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
    if (tokenMeta && headerMeta) {
        return {
            headerName: headerMeta.getAttribute('content'),
            token: tokenMeta.getAttribute('content')
        };
    }
    return null;
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

    montosArchivoInputs.forEach((input) => {
        if (input.value) {
            montosArchivoArray.push(input.value);
        }
    });

    document.querySelectorAll(".movimiento-row").forEach((row) => {
        const listId = row.querySelector(".cuenta-select").getAttribute("list");
        const dataList = document.getElementById(listId);
        const cuentaInput = row.querySelector(".cuenta-select");
        const selectedOption = dataList ? Array.from(dataList.options).find(
            option => option.value === cuentaInput.value
        ) : null;
        const idCuenta = selectedOption ? selectedOption.dataset.id : null;

        const monto = row.querySelector(".monto-input").value
        const tipo = row.querySelector(".tipo-select").value

        if (idCuenta && monto && tipo) {
            movimientos.push({ idCuenta, monto, tipo })
        }
    });

    const formData = new FormData();
    formData.append('concepto', concepto);
    formData.append('fechaPartida', fechaPartida);
    formData.append('movimientos', JSON.stringify(movimientos));
    formData.append("nombresArchivos", JSON.stringify(nombreDocumentosArray));

    // Importante: usar el mismo nombre de parámetro para todos los archivos
    archivosOrigenArray.forEach((archivo, index) => {
        formData.append('archivosOrigen', archivo);
    });

    try {
        const csrf = getCsrf();
        const headers = {};
        if (csrf && csrf.headerName) headers[csrf.headerName] = csrf.token;

        // If no header is available but a CSRF param name exists in meta, append it to the FormData
        if (!(csrf && csrf.headerName) ) {
            const paramMeta = document.querySelector('meta[name="_csrf_param"]');
            const tokenMeta = document.querySelector('meta[name="_csrf"]');
            if (paramMeta && tokenMeta && tokenMeta.getAttribute('content')) {
                const paramName = paramMeta.getAttribute('content');
                const token = tokenMeta.getAttribute('content');
                if (paramName && token) {
                    formData.append(paramName, token);
                }
            }
        }

        const response = await fetch("/partidas/crear", {
            method: "POST",
            headers: headers,
            body: formData
        })

        const data = await response.json();

        if (response.ok) {
            closeModal()  // Cierra la modal antes de la alerta
            await alerta("Partida creada exitosamente")
            location.reload()
        } else {
            const errMsg = data && (data.error || data.message) ? (data.error || data.message) : 'Error al crear la partida';
            await alerta("Error al crear la partida: " + errMsg, 'error')
        }
    } catch (error) {
        await alerta("Error al crear la partida: " + error.message, 'error')
    }
});


async function alerta(message, type = 'info', title = 'OneDi system') {
    return await Swal.fire({
        icon: type,
        title: title,
        text: message,
        confirmButtonText: 'Aceptar',
        customClass: {
            container: 'swal-high-zindex'
        }
    });
}
