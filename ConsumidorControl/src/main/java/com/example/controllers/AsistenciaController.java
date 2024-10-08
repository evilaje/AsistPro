package com.example.controllers;

import com.example.model.Alumno;
import com.example.model.Asistencia;
import com.example.model.DetalleAsistencia;
import com.example.model.Horario;
import com.example.model.Sala;
import com.example.service.IAlumnoService;
import com.example.service.IAsistenciaService;
import com.example.service.IDetalleAsistenciaService;
import com.example.service.IEspecialidadService;
import com.example.service.IHorarioService;
import com.example.service.ISalaService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "asistencias")
public class AsistenciaController {
    @Autowired
    private IAsistenciaService asistenciaService;
    @Autowired 
    private IHorarioService horarioService;
    @Autowired
    private IEspecialidadService especialidadService;
    @Autowired
    private ISalaService salaService;
    @Autowired
    private IAlumnoService alumnoService;
    @Autowired
    private IDetalleAsistenciaService detalleService;
    
    
    @GetMapping("/")
    public String verAsistencias(Model model){
        List<Asistencia> asistencias = asistenciaService.listarAsistencias();
        model.addAttribute("asistencias", asistencias);
        return "verAsistencias";
    }
    
    
    @GetMapping("/nuevaAsistencia")
    public String nuevaAsistencia(Model model) {
        Asistencia asistencia = new Asistencia();
        List<Horario> horarios = horarioService.listarHorarios();
        model.addAttribute("asistencia", asistencia);
        model.addAttribute("horarios", horarios);
        return "formularios/formularioAsistencia";
    }
    
    @PostMapping("/guardarAsistencia")
    public String saveAsistencia(@Valid @ModelAttribute("asistencia") Asistencia asistencia, BindingResult result, Model model) {
        if (result.hasErrors()) {
            List<Horario> horarios = horarioService.listarHorarios();
            model.addAttribute("horarios", horarios);
            return "formulario/formularioAsistencia";
        }
        asistenciaService.guardarAsistencia(asistencia);
        return "redirect:/asistencias/";
    }
    
    @GetMapping("/guardarAsistenciaAutomaticamente/{idLector}/{idAlumno}")
    public String asistenciaAutomatica(@PathVariable("idLector") int idLector, @PathVariable("idAlumno") int idAlumno) {
        Sala salaActual = salaService.buscarPorLector(idLector);
        if (salaActual == null) {
            return "redirect:/error"; // Redireccionar a una página de error si la sala no se encuentra
        }

        LocalTime horaActual = LocalTime.now();
        Horario horario_actual = horarioService.buscarHorariosMasCercanos(salaActual.getId_sala(), horaActual);
        if (horario_actual == null) {
            return "redirect:/no-horario"; // Redireccionar si no hay horario
        }

        LocalDate fechaHoy = LocalDate.now();
        Asistencia asistenciaExistence = asistenciaService.buscarAsistenciaPorFechaYHorario(fechaHoy, horario_actual);

        if (asistenciaExistence == null) {
            Asistencia asistencia = new Asistencia();
            asistencia.setFecha(fechaHoy);
            asistencia.setHorario(horario_actual);
            asistenciaService.guardarAsistencia(asistencia);
            asistenciaExistence = asistencia; // Actualizar la referencia
        }

        Alumno alumno = alumnoService.buscarAlumnoPorID(idAlumno);
        if (alumno == null) {
            return "redirect:/error"; // Redireccionar si el alumno no se encuentra
        }

        DetalleAsistencia detalle = new DetalleAsistencia();
        detalle.setAsistencia(asistenciaExistence);
        detalle.setAlumno(alumno);
        detalle.setHora_presencia(horaActual);

        boolean esTarde = horario_actual.getHora_inicio().plusMinutes(20).isBefore(horaActual);
        detalle.setEsta_presente(!esTarde);

        detalleService.guardarDetalle(detalle);
        return "redirect:/detalle-asistencias/verDetalles/" + asistenciaExistence.getId_asistencia();
    }

    
    
}
