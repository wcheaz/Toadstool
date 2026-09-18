package com.neueda.leap.controller;

import com.neueda.leap.Instrument;
import com.neueda.leap.mapper.InstrumentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {

    @Autowired
    private InstrumentMapper instrumentMapper;

    @GetMapping("/{id}")
    public Instrument getInstrument(@PathVariable UUID id) {
        return instrumentMapper.selectInstrumentById(id);
    }

    @GetMapping("/symbol/{symbol}")
    public Instrument getInstrumentBySymbol(@PathVariable String symbol) {
        return instrumentMapper.selectInstrumentBySymbol(symbol);
    }

    @GetMapping("/tradable")
    public List<Instrument> getTradableInstruments(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return instrumentMapper.selectTradableInstruments(limit, offset);
    }

    @GetMapping("/tradable/count")
    public int countTradable() {
        return instrumentMapper.countTradableInstruments();
    }
}
