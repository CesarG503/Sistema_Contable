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

document.getElementById("archivoOrigen").addEventListener("change", () =>{
    const input = document.getElementById('archivoOrigen');
    const preview = document.getElementById('previewContainer');
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
});

document.getElementById("partidaForm").addEventListener("submit", async (e) => {
    e.preventDefault()

    const concepto = document.getElementById("concepto").value
    const fechaPartida = document.getElementById("fechaPartida").value
    const movimientos = []
    const archivoOrigen = document.getElementById("archivoOrigen").files[0];
    const montoArchivo = document.getElementById("montoArchivo").value;

    document.querySelectorAll(".movimiento-row").forEach((row) => {
        const idCuenta = row.querySelector(".cuenta-select").value
        const monto = row.querySelector(".monto-input").value
        const tipo = row.querySelector(".tipo-select").value

        if (idCuenta && monto && tipo) {
            movimientos.push({ idCuenta, monto, tipo })
        }
    })

    const formData = new FormData();
        formData.append('concepto', concepto);
        formData.append('fechaPartida', fechaPartida);
        formData.append('movimientos', JSON.stringify(movimientos));
        formData.append('archivoOrigen', archivoOrigen);
        formData.append('montoArchivo', montoArchivo);

    try {
        const response = await fetch("/partidas/crear", {
            method: "POST",
            body: formData
        })

        const data = await response.json()

        if (response.ok) {
            alert("Partida creada exitosamente")
            location.reload()
        } else {
            alert(data.error || "Error al crear la partida")
        }
    } catch (error) {
        alert("Error al crear la partida: " + error.message)
    }
});
