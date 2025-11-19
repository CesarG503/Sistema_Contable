package com.example.LoginCliente.Service;

import com.example.LoginCliente.Models.Cuenta;
import com.example.LoginCliente.Models.Movimiento;
import com.example.LoginCliente.Repository.CuentaRepository;
import com.example.LoginCliente.Repository.MovimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class CuentaService {

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    public List<Cuenta> findAll() {
        return cuentaRepository.findAll();
    }

    public List<Cuenta> findByIdEmpresa(Integer idEmpresa) {
        return cuentaRepository.findByIdEmpresa(idEmpresa);
    }

    public Optional<Cuenta> findById(Integer id) {
        return cuentaRepository.findById(id);
    }

    public Cuenta save(Cuenta cuenta) {
        return cuentaRepository.save(cuenta);
    }

    public Map<String, BigDecimal> calcularSaldoCuenta(Integer idCuenta) {
        // Obtener la cuenta para conocer su naturaleza
        Cuenta cuenta = cuentaRepository.findById(idCuenta)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada"));

        List<Movimiento> movimientos = movimientoRepository.findByIdCuenta(idCuenta);

        BigDecimal totalDebe = BigDecimal.ZERO;
        BigDecimal totalHaber = BigDecimal.ZERO;

        for (Movimiento mov : movimientos) {
            if ("D".equals(mov.getTipo())) {
                totalDebe = totalDebe.add(mov.getMonto());
            } else if ("H".equals(mov.getTipo())) {
                totalHaber = totalHaber.add(mov.getMonto());
            }
        }

        // Calcular saldo según la naturaleza de la cuenta
        BigDecimal saldo;
        if ("D".equals(cuenta.getNaturaleza())) {
            saldo = totalDebe.subtract(totalHaber);
        } else {
            saldo = totalHaber.subtract(totalDebe);
        }

        Map<String, BigDecimal> resultado = new HashMap<>();
        resultado.put("debe", totalDebe);
        resultado.put("haber", totalHaber);
        resultado.put("saldo", saldo);

        return resultado;
    }

    public List<Movimiento> obtenerMovimientosPorCuenta(Integer idCuenta) {
        return movimientoRepository.findByIdCuenta(idCuenta);
    }

    public List<Movimiento> obtenerMovimientosPorCuentaYFechas(Integer idCuenta, java.sql.Timestamp fechaInicio, java.sql.Timestamp fechaFin) {
        return movimientoRepository.findByIdCuentaAndDateRange(idCuenta, fechaInicio, fechaFin);
    }

    public Map<String, BigDecimal> calcularEcuacionContable(Integer idEmpresa) {
        List<Cuenta> cuentas = findByIdEmpresa(idEmpresa);

        BigDecimal totalActivo = BigDecimal.ZERO;
        BigDecimal totalPasivo = BigDecimal.ZERO;
        BigDecimal totalCapital = BigDecimal.ZERO;
        BigDecimal totalIngresos = BigDecimal.ZERO;
        BigDecimal totalGastos = BigDecimal.ZERO;

        for (Cuenta cuenta : cuentas) {
            BigDecimal saldoCuenta = calcularSaldoSegunNaturaleza(cuenta);

            String tipo = cuenta.getTipo();
            if (tipo == null) continue;

            switch (tipo.toUpperCase()) {
                case "ACTIVO":
                    totalActivo = totalActivo.add(saldoCuenta);
                    break;
                case "PASIVO":
                    totalPasivo = totalPasivo.add(saldoCuenta);
                    break;
                case "CAPITAL":
                    totalCapital = totalCapital.add(saldoCuenta);
                    break;
                case "INGRESO":
                case "INGRESOS":
                    totalIngresos = totalIngresos.add(saldoCuenta);
                    break;
                case "GASTO":
                case "GASTOS":
                    totalGastos = totalGastos.add(saldoCuenta);
                    break;
            }
        }

        // Capital = Capital Inicial + (Ingresos - Gastos)
        BigDecimal capitalTotal = totalCapital.add(totalIngresos.subtract(totalGastos));

        Map<String, BigDecimal> resultado = new HashMap<>();
        resultado.put("activo", totalActivo);
        resultado.put("pasivo", totalPasivo);
        resultado.put("capital", capitalTotal);
        resultado.put("ingresos", totalIngresos);
        resultado.put("gastos", totalGastos);

        return resultado;
    }

    public Map<String, BigDecimal> calcularEcuacionContableParaPeriodo(Integer idEmpresa, java.sql.Timestamp fechaInicio, java.sql.Timestamp fechaFin) {
        List<Cuenta> cuentas = findByIdEmpresa(idEmpresa);

        BigDecimal totalActivo = BigDecimal.ZERO;
        BigDecimal totalPasivo = BigDecimal.ZERO;
        BigDecimal totalCapital = BigDecimal.ZERO;

        for (Cuenta cuenta : cuentas) {
            BigDecimal saldoCuenta = calcularSaldoSegunNaturezaYFechas(cuenta, fechaInicio, fechaFin);

            String tipo = cuenta.getTipo();
            if (tipo == null) continue;

            switch (tipo.toUpperCase()) {
                case "ACTIVO":
                    totalActivo = totalActivo.add(saldoCuenta);
                    break;
                case "PASIVO":
                    totalPasivo = totalPasivo.add(saldoCuenta);
                    break;
                case "CAPITAL":
                    totalCapital = totalCapital.add(saldoCuenta);
                    break;
            }
        }

        Map<String, BigDecimal> resultado = new HashMap<>();
        resultado.put("activo", totalActivo);
        resultado.put("pasivo", totalPasivo);
        resultado.put("capital", totalCapital);

        return resultado;
    }

    private BigDecimal calcularSaldoSegunNaturaleza(Cuenta cuenta) {
        List<Movimiento> movimientos = movimientoRepository.findByIdCuenta(cuenta.getIdCuenta());

        BigDecimal totalDebe = BigDecimal.ZERO;
        BigDecimal totalHaber = BigDecimal.ZERO;

        for (Movimiento mov : movimientos) {
            if ("D".equals(mov.getTipo())) {
                totalDebe = totalDebe.add(mov.getMonto());
            } else if ("H".equals(mov.getTipo())) {
                totalHaber = totalHaber.add(mov.getMonto());
            }
        }

        // Si la naturaleza es Deudora (D): Saldo = Debe - Haber
        // Si la naturaleza es Acreedora (A): Saldo = Haber - Debe
        if ("D".equals(cuenta.getNaturaleza())) {
            return totalDebe.subtract(totalHaber);
        } else if ("A".equals(cuenta.getNaturaleza())) {
            return totalHaber.subtract(totalDebe);
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal calcularSaldoSegunNaturezaYFechas(Cuenta cuenta, java.sql.Timestamp fechaInicio, java.sql.Timestamp fechaFin) {
        List<Movimiento> movimientos = movimientoRepository.findByIdCuentaAndDateRange(cuenta.getIdCuenta(), fechaInicio, fechaFin);

        BigDecimal totalDebe = BigDecimal.ZERO;
        BigDecimal totalHaber = BigDecimal.ZERO;

        for (Movimiento mov : movimientos) {
            if ("D".equals(mov.getTipo())) {
                totalDebe = totalDebe.add(mov.getMonto());
            } else if ("H".equals(mov.getTipo())) {
                totalHaber = totalHaber.add(mov.getMonto());
            }
        }

        if ("D".equals(cuenta.getNaturaleza())) {
            return totalDebe.subtract(totalHaber);
        } else if ("A".equals(cuenta.getNaturaleza())) {
            return totalHaber.subtract(totalDebe);
        }

        return BigDecimal.ZERO;
    }

    public List<Cuenta> parsearCSV(java.io.InputStream inputStream) throws Exception {
        List<Cuenta> cuentas = new ArrayList<>();

        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream))) {
            String linea;
            boolean primeraLinea = true;

            while ((linea = br.readLine()) != null) {
                // Saltar la cabecera
                if (primeraLinea) {
                    primeraLinea = false;
                    continue;
                }

                if (linea.trim().isEmpty()) continue;

                String[] campos = linea.split(",");
                if (campos.length < 3) continue;

                Cuenta cuenta = new Cuenta();
                cuenta.setNombre(campos[1].trim());
                cuenta.setTipo(campos[2].trim());
                cuenta.setDescripcion(campos.length > 7 ? campos[7].trim() : "");

                cuentas.add(cuenta);
            }
        }

        return cuentas;
    }

    public int guardarCuentasCSV(List<Cuenta> cuentas, Integer idEmpresa) {
        int contador = 0;
        for (Cuenta cuenta : cuentas) {
            cuenta.setIdEmpresa(idEmpresa);
            cuentaRepository.save(cuenta);
            contador++;
        }
        return contador;
    }

    public int agregarCuentasCSV(List<Cuenta> cuentas, Integer idEmpresa) {
        List<Cuenta> cuentasExistentes = findByIdEmpresa(idEmpresa);
        java.util.Set<String> nombresExistentes = new HashSet<>();

        for (Cuenta c : cuentasExistentes) {
            nombresExistentes.add(c.getNombre().toLowerCase());
        }

        int contador = 0;
        for (Cuenta cuenta : cuentas) {
            if (!nombresExistentes.contains(cuenta.getNombre().toLowerCase())) {
                cuenta.setIdEmpresa(idEmpresa);
                cuentaRepository.save(cuenta);
                contador++;
            }
        }
        return contador;
    }

    public void borrarCuentasEmpresa(Integer idEmpresa) {
        List<Cuenta> cuentas = findByIdEmpresa(idEmpresa);
        cuentaRepository.deleteAll(cuentas);
    }
    @Transactional
    public Map<String, Object> deleteById(Integer id) {
        Map<String, Object> resultado = new HashMap<>();

        if (!cuentaRepository.existsById(id)) {
            resultado.put("success", false);
            resultado.put("message", "La cuenta no existe");
            return resultado;
        }

        List<Movimiento> movimientos = movimientoRepository.findByIdCuenta(id);

        if (!movimientos.isEmpty()) {
            resultado.put("success", false);
            resultado.put("message", "No se puede eliminar la cuenta porque tiene " +
                    movimientos.size() + " movimiento(s) asociado(s)");
            resultado.put("cantidadMovimientos", movimientos.size());
            return resultado;
        }

        cuentaRepository.deleteById(id);
        resultado.put("success", true);
        resultado.put("message", "Cuenta eliminada exitosamente");
        return resultado;
    }
}
