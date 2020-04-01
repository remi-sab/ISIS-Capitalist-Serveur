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
            File file = new File(username + "-world.xml");
            //input = new FileInputStream(file);
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
            OutputStream output = new FileOutputStream(username + "-" + "world.xml");
            cont = JAXBContext.newInstance(World.class);
            Marshaller m = cont.createMarshaller();
            m.marshal(world, output);
            System.out.println("J'enregistre la partie : " + output);
            output.close();

        } catch (Exception ex) {
            System.out.println("Erreur : " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public World getWorld(String username) throws JAXBException, FileNotFoundException, IOException {
        World world = readWorldFromXml(username);
        saveWorldToXml(world, username);
        return world;
        /*for (ProductType prod : world.getProducts().getProduct()) {
            if (!prod.isManagerUnlocked()) {
                if (prod.getTimeleft() != 0) {
                    if (prod.getTimeleft() < (System.currentTimeMillis() - world.getLastupdate())) {
                        world.setScore(world.getScore() + prod.getRevenu());
                    } else {
                        prod.setTimeleft(prod.getTimeleft() - (System.currentTimeMillis() - world.getLastupdate()));
                    }
                }
            } else {
                long time = System.currentTimeMillis() - world.getLastupdate();
                long nb_prod = (time / prod.getVitesse());
                long time_left = (time % prod.getVitesse());
                world.setScore(world.getScore() + prod.getRevenu() * nb_prod);
                prod.setTimeleft(time_left);
            }
        }
        
        world.setLastupdate(System.currentTimeMillis());
        saveWorldToXml(world, username);
        return this.readWorldFromXml(username);*/
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

    public void updateWorld(World world) {
        Long derniereMaj = world.getLastupdate();
        Long maintenant = System.currentTimeMillis();
        Long delta = maintenant - derniereMaj;
        int angeBonus = world.getAngelbonus();
        List<ProductType> pt = (List<ProductType>) world.getProducts();
        for (ProductType a : pt) {
            if (a.isManagerUnlocked()) {
                int tempsProduit = a.getVitesse();
                int nbrePd = (int) (delta / tempsProduit);
                long restant = a.getVitesse() - delta % tempsProduit;
                double argent = a.getRevenu() * nbrePd * (1 + world.getActiveangels() * angeBonus / 100);
                world.setMoney(world.getMoney() + argent);
                world.setScore(world.getScore() + argent);
                a.setTimeleft(restant);
            } else {
                if (a.getTimeleft() != 0 && a.getTimeleft() < delta) {
                    double argent = a.getRevenu();
                    world.setMoney(world.getMoney() + argent);
                    world.setScore(world.getScore() + argent);

                }
                a.setTimeleft(a.getTimeleft() - delta);
            }
        }
        world.setLastupdate(System.currentTimeMillis());
    }

    // prend en paramètre le pseudo du joueur et le produit 
    // sur lequel une action a eu lieu (lancement manuel de production ou 
    // achat d’une certaine quantité de produit) 
    // renvoie false si l’action n’a pas pu être traitée 
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
            //double prix= newproduct.cout*qtchange;
            double prix1 = product.getCout();
            double prix2 = prix1 * ((1 - (Math.pow(q, qtchange))) / (1 - q));
            double argentRestant = argent - prix2;
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

    /*// aller chercher le monde qui correspond au joueur 
        World world = getWorld(username); 
        // trouver dans ce monde, le produit équivalent à celui passé en paramètre 
        ProductType product = findProductById(world, newproduct.getId()); 
        if (product == null) { return false;} 
        // calculer la variation de quantité. Si elle est positive c'est 
        // que le joueur a acheté une certaine quantité de ce produit 
        // sinon c’est qu’il s’agit d’un lancement de production. 
        int qtchange = newproduct.getQuantite() - product.getQuantite(); 
        if (qtchange > 0) { 
            // soustraire de l'argent du joueur le cout de la quantité 
            // achetée et mettre à jour la quantité de product 
            double cout = (product.getCout() * (1 - Math.pow(product.getCroissance(), product.getQuantite()))) / (1 - product.getCroissance());
            world.setMoney(world.getMoney() - cout);
            product.setQuantite(product.getQuantite() + newproduct.getQuantite());
        } else { 
            // initialiser product.timeleft à product.vitesse 
            // pour lancer la production 
            product.setTimeleft(product.getVitesse());
            product.setQuantite(newproduct.getQuantite());
        } 
        // sauvegarder les changements du monde 
        saveWorldToXml(world, username); 
        return true;*/

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
        if (world.getMoney() >= upgrade.getSeuil()) {
            if (upgrade.getIdcible() == 0) {
                List<ProductType> listeProduits = world.getProducts().getProduct();
                for (ProductType p : listeProduits) {
                    majPallier(upgrade, p);
                }
                return true;
            } else {
                ProductType p = findProductById(world, upgrade.getIdcible());
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
            int angeBonus = world.getAngelbonus();
            angeBonus += angeBonus + ange.getRatio();
            world.setAngelbonus(angeBonus);
            //demander
        } else {
            updateUpgrades(username, ange);
        }
        world.setActiveangels(newAngeActif);
    }

    /*private PallierType findUpgradeByName(World world, String name) {
        PallierType upgrade = null;
        for (PallierType p : world.getUpgrades().getPallier()) {
            if (p.getName().equals(name)) {
                upgrade = p;
            }
        }
        return upgrade;
    }*/
}
