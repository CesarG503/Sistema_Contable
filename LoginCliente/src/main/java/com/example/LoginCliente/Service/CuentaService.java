package com.example.LoginCliente.Service;

import com.example.LoginCliente.Models.Cuenta;
import com.example.LoginCliente.Models.Movimiento;
import com.example.LoginCliente.Repository.CuentaRepository;
import com.example.LoginCliente.Repository.MovimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

        Map<String, BigDecimal> resultado = new HashMap<>();
        resultado.put("debe", totalDebe);
        resultado.put("haber", totalHaber);
        resultado.put("saldo", totalDebe.subtract(totalHaber));

        return resultado;
    }

    public List<Movimiento> obtenerMovimientosPorCuenta(Integer idCuenta) {
        return movimientoRepository.findByIdCuenta(idCuenta);
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
}
