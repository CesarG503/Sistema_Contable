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
}
