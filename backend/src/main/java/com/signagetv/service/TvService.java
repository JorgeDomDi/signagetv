package com.signagetv.service;

import com.signagetv.dto.PlaylistDto;
import com.signagetv.dto.TvDto;
import com.signagetv.dto.TvRegisterRequest;
import com.signagetv.entity.Playlist;
import com.signagetv.entity.Schedule;
import com.signagetv.entity.Tv;
import com.signagetv.exception.BadRequestException;
import com.signagetv.exception.NotFoundException;
import com.signagetv.repository.PlaylistRepository;
import com.signagetv.repository.ScheduleRepository;
import com.signagetv.repository.TvRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TvService {

    private final TvRepository tvRepo;
    private final PlaylistRepository playlistRepo;
    private final ScheduleRepository scheduleRepo;
    private final PlaylistService playlistService;
    private final RealtimeNotificationService realtime;

    public List<TvDto> list(Long localId) {
        return tvRepo.findByLocalIdOrderByNombreAsc(localId).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Registro idempotente por (localId, deviceId). Si ya existe la TV con ese deviceId
     * para ese local, devuelve la existente actualizando el nombre.
     */
    @Transactional
    public TvDto register(Long localId, TvRegisterRequest req) {
        Optional<Tv> existing = tvRepo.findByLocalIdAndDeviceId(localId, req.getDeviceId());
        Tv tv = existing.orElseGet(() -> Tv.builder()
                .localId(localId)
                .deviceId(req.getDeviceId())
                .nombre(req.getNombre())
                .online(true)
                .build());

        tv.setNombre(req.getNombre());
        tv.setOnline(true);
        tv.setLastSeen(LocalDateTime.now());
        tv = tvRepo.save(tv);
        log.info("TV registrada id={} local={} device={}", tv.getId(), localId, tv.getDeviceId());
        return toDto(tv);
    }

    @Transactional
    public TvDto assignPlaylist(Long localId, Long tvId, Long playlistId) {
        Tv tv = tvRepo.findByIdAndLocalId(tvId, localId)
                .orElseThrow(() -> new NotFoundException("TV no encontrada"));

        if (playlistId != null) {
            playlistRepo.findByIdAndLocalId(playlistId, localId)
                    .orElseThrow(() -> new BadRequestException("La playlist no pertenece a este local"));
        }
        tv.setCurrentPlaylistId(playlistId);
        tv = tvRepo.save(tv);

        realtime.notifyPlaylistsChanged(localId);
        realtime.sendTvCommand(tv.getId(), "REFRESH", null);
        return toDto(tv);
    }

    /**
     * Resuelve qué playlist debería estar reproduciendo la TV en este instante.
     *  1. Schedule activo (mayor prioridad) cuyo día y franja horaria coincida → su playlist.
     *  2. Si no hay schedule → tv.current_playlist_id (selección manual).
     *  3. Si tampoco → null.
     */
    public PlaylistDto getCurrentPlaylist(Long localId, Long tvId) {
        Tv tv = tvRepo.findByIdAndLocalId(tvId, localId)
                .orElseThrow(() -> new NotFoundException("TV no encontrada"));
        return resolveCurrent(tv);
    }

    /**
     * Variante usada por la app TV: resuelve la TV por deviceId (header X-Device-Id)
     * dentro del local autenticado. Actualiza last_seen + online.
     */
    @Transactional
    public PlaylistDto getCurrentPlaylistForDevice(Long localId, String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new BadRequestException("Header X-Device-Id requerido");
        }
        Tv tv = tvRepo.findByLocalIdAndDeviceId(localId, deviceId)
                .orElseThrow(() -> new NotFoundException("TV no registrada para deviceId=" + deviceId));
        tv.setLastSeen(LocalDateTime.now());
        tv.setOnline(true);
        tvRepo.save(tv);
        return resolveCurrent(tv);
    }

    // ------------------------------------------------------------------------

    private PlaylistDto resolveCurrent(Tv tv) {
        // 1) Schedule activo
        Optional<Schedule> active = pickActiveSchedule(tv.getLocalId(), LocalDateTime.now());
        if (active.isPresent()) {
            Playlist p = playlistRepo.findByIdAndLocalId(active.get().getPlaylistId(), tv.getLocalId())
                    .orElse(null);
            if (p != null) return playlistService.buildPlaylistDto(p);
        }
        // 2) Selección manual
        if (tv.getCurrentPlaylistId() != null) {
            Playlist p = playlistRepo.findByIdAndLocalId(tv.getCurrentPlaylistId(), tv.getLocalId())
                    .orElse(null);
            if (p != null) return playlistService.buildPlaylistDto(p);
        }
        // 3) Nada
        return null;
    }

    private Optional<Schedule> pickActiveSchedule(Long localId, LocalDateTime now) {
        DayOfWeek today = now.getDayOfWeek();
        LocalTime time = now.toLocalTime();
        String todayCode = dayCode(today);

        return scheduleRepo.findByLocalIdAndActivoTrue(localId).stream()
                .filter(s -> diasContains(s.getDiasSemana(), todayCode))
                .filter(s -> !time.isBefore(s.getHoraInicio()) && !time.isAfter(s.getHoraFin()))
                .max(Comparator.comparingInt(Schedule::getPrioridad)
                        .thenComparing(Comparator.comparing(Schedule::getId).reversed()));
    }

    private boolean diasContains(String csv, String code) {
        if (csv == null || csv.isBlank()) return false;
        Set<String> set = Arrays.stream(csv.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        return set.contains(code);
    }

    private String dayCode(DayOfWeek d) {
        return switch (d) {
            case MONDAY    -> "MON";
            case TUESDAY   -> "TUE";
            case WEDNESDAY -> "WED";
            case THURSDAY  -> "THU";
            case FRIDAY    -> "FRI";
            case SATURDAY  -> "SAT";
            case SUNDAY    -> "SUN";
        };
    }

    private TvDto toDto(Tv tv) {
        return TvDto.builder()
                .id(tv.getId())
                .nombre(tv.getNombre())
                .deviceId(tv.getDeviceId())
                .currentPlaylistId(tv.getCurrentPlaylistId())
                .lastSeen(tv.getLastSeen())
                .online(tv.getOnline())
                .build();
    }
}
