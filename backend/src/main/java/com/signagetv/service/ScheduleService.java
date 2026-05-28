package com.signagetv.service;

import com.signagetv.dto.ScheduleDto;
import com.signagetv.dto.ScheduleRequest;
import com.signagetv.entity.Schedule;
import com.signagetv.exception.BadRequestException;
import com.signagetv.exception.NotFoundException;
import com.signagetv.repository.PlaylistRepository;
import com.signagetv.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepo;
    private final PlaylistRepository playlistRepo;
    private final RealtimeNotificationService realtime;

    public List<ScheduleDto> list(Long localId) {
        return scheduleRepo.findByLocalIdOrderByPrioridadDescIdAsc(localId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ScheduleDto create(Long localId, ScheduleRequest req) {
        validatePlaylistOwnership(localId, req.getPlaylistId());
        validateRequest(req);

        Schedule s = Schedule.builder()
                .localId(localId)
                .playlistId(req.getPlaylistId())
                .nombre(req.getNombre())
                .diasSemana(req.getDiasSemana())
                .horaInicio(req.getHoraInicio())
                .horaFin(req.getHoraFin())
                .activo(req.getActivo() == null ? Boolean.TRUE : req.getActivo())
                .prioridad(req.getPrioridad() == null ? 0 : req.getPrioridad())
                .build();
        s = scheduleRepo.save(s);
        realtime.notifyPlaylistsChanged(localId);
        return toDto(s);
    }

    @Transactional
    public ScheduleDto update(Long localId, Long id, ScheduleRequest req) {
        Schedule s = scheduleRepo.findByIdAndLocalId(id, localId)
                .orElseThrow(() -> new NotFoundException("Schedule no encontrado"));

        if (req.getPlaylistId() != null) {
            validatePlaylistOwnership(localId, req.getPlaylistId());
            s.setPlaylistId(req.getPlaylistId());
        }
        if (req.getNombre() != null) s.setNombre(req.getNombre());
        if (req.getDiasSemana() != null) s.setDiasSemana(req.getDiasSemana());
        if (req.getHoraInicio() != null) s.setHoraInicio(req.getHoraInicio());
        if (req.getHoraFin() != null) s.setHoraFin(req.getHoraFin());
        if (req.getActivo() != null) s.setActivo(req.getActivo());
        if (req.getPrioridad() != null) s.setPrioridad(req.getPrioridad());

        validateTimes(s.getHoraInicio(), s.getHoraFin());

        s = scheduleRepo.save(s);
        realtime.notifyPlaylistsChanged(localId);
        return toDto(s);
    }

    @Transactional
    public void delete(Long localId, Long id) {
        Schedule s = scheduleRepo.findByIdAndLocalId(id, localId)
                .orElseThrow(() -> new NotFoundException("Schedule no encontrado"));
        scheduleRepo.delete(s);
        realtime.notifyPlaylistsChanged(localId);
    }

    private void validatePlaylistOwnership(Long localId, Long playlistId) {
        playlistRepo.findByIdAndLocalId(playlistId, localId)
                .orElseThrow(() -> new BadRequestException("La playlist no pertenece a este local"));
    }

    private void validateRequest(ScheduleRequest req) {
        if (req.getDiasSemana() == null || req.getDiasSemana().isBlank()) {
            throw new BadRequestException("diasSemana requerido");
        }
        validateTimes(req.getHoraInicio(), req.getHoraFin());
    }

    private void validateTimes(java.time.LocalTime ini, java.time.LocalTime fin) {
        if (ini == null || fin == null) throw new BadRequestException("horaInicio y horaFin requeridos");
        if (!fin.isAfter(ini)) throw new BadRequestException("horaFin debe ser posterior a horaInicio");
    }

    private ScheduleDto toDto(Schedule s) {
        return ScheduleDto.builder()
                .id(s.getId())
                .playlistId(s.getPlaylistId())
                .nombre(s.getNombre())
                .diasSemana(s.getDiasSemana())
                .horaInicio(s.getHoraInicio())
                .horaFin(s.getHoraFin())
                .activo(s.getActivo())
                .prioridad(s.getPrioridad())
                .build();
    }
}
