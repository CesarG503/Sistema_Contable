function openModal() {
    document.getElementById("partidaModal").style.display = "block"
}

function closeModal() {
    document.getElementById("partidaModal").style.display = "none"
    document.getElementById("partidaForm").reset()
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
    <label for="archivoOrigen${nDocumentos}" class="file-label">Subir archivo</label>
        <input type="file" name="archivoOrigen${nDocumentos}" id="archivoOrigen${nDocumentos}" accept=".pdf,image/*" class="file-input archivo-origen" required>
    <div class="preview-container" id="previewContainer${nDocumentos}">
        <p>Ningún archivo seleccionado</p>
    </div>
    <div class="form-group">
        <label for="montoArchivo${nDocumentos}">Monto del archivo</label>
        <input type="number" step="0.01" min="0" name="montoArchivo${nDocumentos}" id="montoArchivo${nDocumentos}" class="monto-archivo" placeholder="$0.00" required>
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
    if (nDocumentos <= 1) {
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
}

document.getElementById("archivoOrigen").addEventListener("change", () =>{
    cargarVistaPrevia('');
});

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
        const selectedOption = Array.from(dataList.options).find(
            option => option.value === cuentaInput.value
        );
        const idCuenta = selectedOption.dataset.id;

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

    // Debug: mostrar cuántos archivos se van a enviar
    console.log("=== DEBUG JS ===");
    console.log("Archivos a enviar:", archivosOrigenArray.length);

    // Importante: usar el mismo nombre de parámetro para todos los archivos
    archivosOrigenArray.forEach((archivo, index) => {
        console.log(`Añadiendo archivo ${index}:`, archivo.name);
        formData.append('archivosOrigen', archivo);
    });

    formData.append('montosArchivo', JSON.stringify(montosArchivoArray));

    // Debug: mostrar el contenido del FormData
    console.log("Contenido del FormData:");
    for (let pair of formData.entries()) {
        console.log(pair[0] + ': ' + (pair[1] instanceof File ? pair[1].name : pair[1]));
    }

    try {
        const response = await fetch("/partidas/crear", {
            method: "POST",
            body: formData
        })

        const data = await response.json()

        if (response.ok) {
            closeModal()  // Cierra la modal antes de la alerta
            await alerta("Partida creada exitosamente")
            location.reload()
        } else {
            closeModal()
            await alerta("Error al crear la partida: " + error.message, 'error')
        }
    } catch (error) {
        closeModal()
        await alerta("Error al crear la partida: " + error.message, 'error')
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
