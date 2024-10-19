package com.aluracursos.screenmatch.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aluracursos.screenmatch.dto.SerieDTO;
import com.aluracursos.screenmatch.repository.SerieRepository;



@RestController
//@RequestMapping("api/v1")
public class SerieController {

    @Autowired
    private SerieRepository repository;  

    @GetMapping("/series")
    public List<SerieDTO>  obtenerTodasLasSeries() {
        return repository.findAll().stream().map(s -> new SerieDTO(s.getTitulo(),
        s.getTotalTemporadas(), s.getEvaluacion(), s.getPoster(),
        s.getGenero(), s.getActores(), s.getSinopsis()))
        .collect(Collectors.toList());
    }
    

}
