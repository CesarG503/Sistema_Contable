package com.example.LoginCliente.Controller;

import com.example.LoginCliente.Models.Cuenta;
import com.example.LoginCliente.Models.Movimiento;
import com.example.LoginCliente.Models.Partida;
import com.example.LoginCliente.Models.PartidaDTO;
import com.example.LoginCliente.Models.Usuario;
import com.example.LoginCliente.Service.CuentaService;
import com.example.LoginCliente.Service.PartidaService;
import com.example.LoginCliente.Service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/partidas")
public class PartidaController {

    @Autowired
    private PartidaService partidaService;

    @Autowired
    private CuentaService cuentaService;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/libro-diario")
    public String librodiario(Model model, HttpSession session) {
        Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
        if (empresaActiva == null) {
            return "redirect:/empresas/mis-empresas";
        }

        List<Partida> partidas = partidaService.findByIdEmpresa(empresaActiva);
        List<Cuenta> cuentas = cuentaService.findByIdEmpresa(empresaActiva);

        Map<PartidaDTO, List<Movimiento>> partidasConMovimientos = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (Partida partida : partidas) {
            String fechaFormateada = "";
            if (partida.getFecha() != null) {
                if (partida.getFecha() instanceof Timestamp) {
                    fechaFormateada = ((Timestamp) partida.getFecha()).toLocalDateTime().format(formatter);
                } else if (partida.getFecha() instanceof java.util.Date) {
                    fechaFormateada = new Timestamp(((java.util.Date) partida.getFecha()).getTime()).toLocalDateTime().format(formatter);
                } else {
                    fechaFormateada = partida.getFecha().toString();
                }
            }
            String autorStr = partida.getAutor() != null ? partida.getAutor().toString() : "";
            PartidaDTO partidaDTO = new PartidaDTO(
                    partida.getIdPartida(),
                    partida.getConcepto(),
                    fechaFormateada,
                    autorStr
            );
            List<Movimiento> movimientos = partidaService.findMovimientosByPartida(partida.getIdPartida());
            partidasConMovimientos.put(partidaDTO, movimientos);
        }

        model.addAttribute("partidasConMovimientos", partidasConMovimientos);
        model.addAttribute("cuentas", cuentas);
        model.addAttribute("nuevaPartida", new Partida());
        model.addAttribute("page", "libro-diario");

        return "libro-diario";
    }

    @PostMapping("/crear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearPartida(@RequestBody Map<String, Object> datos, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
            Integer usuarioEmpresaId = (Integer) session.getAttribute("usuarioEmpresaId");

            if (empresaActiva == null || usuarioEmpresaId == null) {
                response.put("error", "Debe seleccionar una empresa primero");
                return ResponseEntity.badRequest().body(response);
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            Usuario usuario = usuarioService.findByUsuario(username);

            Partida partida = new Partida();
            partida.setConcepto((String) datos.get("concepto"));
            // Obtener la fecha enviada por el usuario
            String fechaStr = (String) datos.get("fechaPartida");
            if (fechaStr != null && !fechaStr.isEmpty()) {
                // Convertir la fecha (formato yyyy-MM-dd) a Timestamp
                LocalDateTime fecha = LocalDateTime.parse(fechaStr + "T00:00:00");
                partida.setFecha(Timestamp.valueOf(fecha));
            } else {
                partida.setFecha(new Timestamp(System.currentTimeMillis()));
            }
            partida.setAutor(usuario.getIdUsuario());
            partida.setIdEmpresa(empresaActiva);
            partida.setIdUsuarioEmpresa(usuarioEmpresaId);

            List<Movimiento> movimientos = new ArrayList<>();
            List<Map<String, Object>> movimientosData = (List<Map<String, Object>>) datos.get("movimientos");

            BigDecimal totalDebe = BigDecimal.ZERO;
            BigDecimal totalHaber = BigDecimal.ZERO;

            for (Map<String, Object> movData : movimientosData) {
                Movimiento movimiento = new Movimiento();
                movimiento.setIdCuenta(Integer.parseInt(movData.get("idCuenta").toString()));
                movimiento.setMonto(new BigDecimal(movData.get("monto").toString()));
                movimiento.setTipo((String) movData.get("tipo"));
                movimiento.setIdEmpresa(empresaActiva);
                movimiento.setIdUsuarioEmpresa(usuarioEmpresaId);

                if ("D".equals(movimiento.getTipo())) {
                    totalDebe = totalDebe.add(movimiento.getMonto());
                } else {
                    totalHaber = totalHaber.add(movimiento.getMonto());
                }

                movimientos.add(movimiento);
            }

            if (totalDebe.compareTo(totalHaber) != 0) {
                response.put("error", "El total del Debe debe ser igual al total del Haber");
                return ResponseEntity.badRequest().body(response);
            }

            Partida savedPartida = partidaService.save(partida, movimientos);
            response.put("success", true);
            response.put("partidaId", savedPartida.getIdPartida());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Error al crear la partida: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{idPartida}/movimientos")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getMovimientos(@PathVariable Integer idPartida, HttpSession session) {
        try {
            Integer empresaActiva = (Integer) session.getAttribute("empresaActiva");
            if (empresaActiva == null) {
                return ResponseEntity.badRequest().build();
            }

            List<Movimiento> movimientos = partidaService.findMovimientosByPartida(idPartida);
            List<Cuenta> cuentas = cuentaService.findByIdEmpresa(empresaActiva);

            List<Map<String, Object>> movimientosConNombres = new ArrayList<>();

            for (Movimiento mov : movimientos) {
                Map<String, Object> movData = new HashMap<>();
                movData.put("monto", mov.getMonto());
                movData.put("tipo", mov.getTipo());

                // Find account name
                String nombreCuenta = "";
                for (Cuenta cuenta : cuentas) {
                    if (cuenta.getIdCuenta().equals(mov.getIdCuenta())) {
                        nombreCuenta = cuenta.getNombre();
                        break;
                    }
                }
                movData.put("nombreCuenta", nombreCuenta);

                movimientosConNombres.add(movData);
            }

            return ResponseEntity.ok(movimientosConNombres);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
