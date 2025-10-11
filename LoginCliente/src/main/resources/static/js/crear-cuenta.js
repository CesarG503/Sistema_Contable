function setNaturaleza() {
    const tipo = document.getElementById('tipo').value;
    const naturalezaHidden = document.getElementById('naturaleza');
    const naturalezaDisplay = document.getElementById('naturaleza-display');

    const naturalezaMap = {
        'Activo': { value: 'D', display: 'Deudora' },
        'Pasivo': { value: 'A', display: 'Acreedora' },
        'Capital': { value: 'A', display: 'Acreedora' },
        'Ingreso': { value: 'A', display: 'Acreedora' },
        'Gasto': { value: 'D', display: 'Deudora' }
    };

    if (naturalezaMap[tipo]) {
        naturalezaHidden.value = naturalezaMap[tipo].value;
        naturalezaDisplay.value = naturalezaMap[tipo].display;
    } else {
        naturalezaHidden.value = '';
        naturalezaDisplay.value = '';
    }
}