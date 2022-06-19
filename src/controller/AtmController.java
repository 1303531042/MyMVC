package controller;

import domain.User;
import service.AtmService;

public class AtmController {
    private AtmService atmService = new AtmService();

    public String AZC(User user) {
        return atmService.ZC(user);
    }
}
