package service;

import domain.Venda;

import java.util.ArrayList;
import java.util.List;

public class VendasService {

    private static final List<Venda> vendas = new ArrayList<>();

    private static final VendasService instance = new VendasService();

    public static VendasService getInstance() {
        return instance;
    }

    private VendasService() {}

    public List<Venda> getVendas() {
        return vendas;
    }
}
