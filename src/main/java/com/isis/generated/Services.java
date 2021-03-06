/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.isis.generated;

import static com.isis.generated.TyperatioType.ANGE;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 *
 * @author remis
 */
public class Services {

    static World world = new World();
    InputStream input = getClass().getClassLoader().getResourceAsStream("world.xml");

    public World readWorldFromXml(String username) throws JAXBException, FileNotFoundException {

        JAXBContext cont;
        
        try {
            File file = new File(username + "-" + "world.xml");
            cont = JAXBContext.newInstance(World.class);
            Unmarshaller u = cont.createUnmarshaller();
            world = (World) u.unmarshal(file);
            System.out.println("J'ouvre le fichier : " + file.getAbsolutePath());
            return world;

        } catch (Exception e) {
            cont = JAXBContext.newInstance(World.class);
            Unmarshaller u = cont.createUnmarshaller();
            world = (World) u.unmarshal(input);
            System.out.println("Bienvenue au nouveau joueur : " + username);
            return world;
        }      
    }

    public void saveWorldToXml(World world, String username) throws JAXBException, FileNotFoundException, IOException {

        JAXBContext cont;

        try {
            File file = new File(username + "-" + "world.xml");
            cont = JAXBContext.newInstance(World.class);
            Marshaller m = cont.createMarshaller();
            m.marshal(world, file);
            System.out.println("J'enregistre la partie : " + file.getAbsolutePath());

        } catch (Exception ex) {
            System.out.println("Erreur : " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public World getWorld(String username) throws JAXBException, FileNotFoundException, IOException {
        World world = readWorldFromXml(username);
        long timeCurrent = System.currentTimeMillis();
        long LastTime = world.getLastupdate();
 
        if (LastTime == timeCurrent){
            return world;
        } 
        else {
        // Mise à jour du Score
        world = updateWorld(world);
        world.setLastupdate(timeCurrent); 
        saveWorldToXml(world,username);
        return world;
        }
    }

    public void deleteWorld(String username) throws JAXBException, FileNotFoundException, IOException {
        World monde = readWorldFromXml(username);
        double angeActif = monde.getActiveangels();
        double angeTotal = monde.getTotalangels();
        double aRajouter = nombreAnges(monde);
        angeActif += angeActif + aRajouter;
        angeTotal += angeTotal + aRajouter;
        double score = monde.getScore();
       
        InputStream input = getClass().getClassLoader().getResourceAsStream("world.xml");
        JAXBContext cont = JAXBContext.newInstance(World.class);
        Unmarshaller u = cont.createUnmarshaller();
        World world = (World) u.unmarshal(input);
        world.setActiveangels(angeActif);
        world.setTotalangels(angeTotal);
        world.setScore(score);

        saveWorldToXml(world, username);

    }

    public double nombreAnges(World world) throws JAXBException, FileNotFoundException, IOException {
        double nombreAnges = world.getTotalangels();
        double angeToClaim = Math.round(150 * Math.sqrt((world.getScore()) / Math.pow(10, 15))) - nombreAnges;
        return angeToClaim;
    }

    public World updateWorld(World world) {
        long timeSinceUpdate = System.currentTimeMillis()-world.getLastupdate();
        double angeBonus = Math.pow(world.getAngelbonus(),world.getActiveangels());

        for (ProductType product : world.getProducts().getProduct()) {
            long timeleft = product.getTimeleft();
            if (timeleft != 0) {
                if(timeleft<timeSinceUpdate){
                    world.setScore(world.getScore() + (product.getRevenu() * angeBonus));
                    world.setMoney(world.getMoney() + (product.getRevenu() * angeBonus));
                    timeSinceUpdate -= timeleft;
                } else {
                    product.setTimeleft(timeleft - timeSinceUpdate);
                    timeSinceUpdate = 0;
                }
            }
            if (product.isManagerUnlocked()) {
                //Calcul de l'argent gagné
                int productionTime = product.getVitesse();
                int nbProduit = (int) (timeSinceUpdate / productionTime);
                double argent = product.getRevenu() * nbProduit * angeBonus;
                //Mise à jour score et argent
                world.setMoney(world.getMoney() + argent);
                world.setScore(world.getScore() + argent);
                //Calcul du temps restant sur la production du produit
                long tempsrestant = productionTime * (nbProduit + 1) - timeSinceUpdate;
                product.setTimeleft(tempsrestant);
            }
        }
      return world;
    }

    public Boolean updateProduct(String username, ProductType newproduct) throws JAXBException, IOException {

        World world = getWorld(username);
        ProductType product = findProductById(world, newproduct.getId());
        if (product == null) {
            return false;
        }
        int qtchange = newproduct.getQuantite() - product.getQuantite();
        if (qtchange > 0) {
            double argent = world.getMoney();
            double q = product.getCroissance();
            double prix = product.getCout() * ((1 - (Math.pow(q, qtchange))) / (1 - q));
            double argentRestant = argent - prix;
            world.setMoney(argentRestant);
            product.setQuantite(newproduct.getQuantite());
        } else {
            product.timeleft = product.vitesse;
        }
        List<PallierType> t = (List<PallierType>) product.getPalliers().getPallier();
        for (PallierType a : t) {
            if (a.isUnlocked() == false && product.getQuantite() >= a.getSeuil()) {
                a.setUnlocked(true);
                if (a.getTyperatio() == TyperatioType.VITESSE) {
                    int b = product.getVitesse();
                    b = (int) (b * a.getRatio());
                    product.setVitesse(b);
                } else {
                    double c = product.getRevenu();
                    c = c * a.getRatio();
                    product.setRevenu(c);
                }
            }
        }
        saveWorldToXml(world, username);
        return true;
    }

    private ProductType findProductById(World world, int id) {
        ProductType product = null;
        for (ProductType p : world.getProducts().getProduct()) {
            if (p.getId() == id) {
                product = p;
            }
        }
        return product;
    }

    public Boolean updateManager(String username, PallierType newmanager) throws JAXBException, FileNotFoundException, IOException {

        World world = getWorld(username);
        PallierType manager = findManagerByName(world, newmanager.getName());

        if (manager == null) {
            return false;
        }
        manager.setUnlocked(true);
        ProductType product = findProductById(world, manager.getIdcible());
        if (product == null) {
            return false;
        }

        product.setManagerUnlocked(true);

        world.setMoney(world.getMoney() - manager.getSeuil());
        saveWorldToXml(world, username);
        return true;
    }

    private PallierType findManagerByName(World world, String name) {
        PallierType manager = null;
        for (PallierType palier : world.getManagers().getPallier()) {
            if (palier.getName().equals(name)) {
                manager = palier;
            }
        }
        return manager;
    }

    public boolean updateUpgrades(String username, PallierType upgrade) throws JAXBException, IOException {
        World world = getWorld(username);
           if(upgrade.getIdcible() == 0){
            boolean allunlocks = true;
            for (ProductType p : world.getProducts().getProduct()){
                if(p.getQuantite()< upgrade.getSeuil()){
                    allunlocks=false;
                }
            }
            if(allunlocks){
                for (ProductType p : world.getProducts().getProduct()){
                    majPallier(upgrade, p);
                }
            }
            return true;
        } else {
            ProductType p = findProductById(world, upgrade.getIdcible());
            if(p.getQuantite()>upgrade.getSeuil()){
                majPallier(upgrade, p);
                return true;
            }
        }
        return false;
   }

    public void majPallier(PallierType pt, ProductType p) {
        pt.setUnlocked(true);
        if (pt.getTyperatio() == TyperatioType.VITESSE) {
            double v = p.getVitesse();
            v = (int) (v * pt.getRatio());
            p.setVitesse((int) v);

        }
        if (pt.getTyperatio() == TyperatioType.GAIN) {
            double c = p.getRevenu();
            c = c * pt.getRatio();
            p.setRevenu(c);
        }
    }

    public void angelUpgrade(String username, PallierType ange) throws JAXBException, IOException {
        int a = ange.getSeuil();
        World world = getWorld(username);
        double angeActif = world.getActiveangels();
        double newAngeActif = angeActif - a;
        if (ange.getTyperatio() == TyperatioType.ANGE) {
            double angeBonus = world.getAngelbonus();
            angeBonus += angeBonus + ange.getRatio();
            world.setAngelbonus(angeBonus);
        } else {
            updateUpgrades(username, ange);
        }
        world.setActiveangels(newAngeActif);
    }

}
