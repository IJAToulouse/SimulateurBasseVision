package ija.lbled.simulateurbassevision;

/**
 * Created by l.bled on 28/05/2015.
 */
public class Modèle {
    private String acuite;
    private String distance;
    private int scotome;
    private boolean isScotome;
    private int tubulaire;
    private boolean isTubulaire;
    private int hemianopsie;
    private boolean isHemianopsie;
    private int contraste;
    private int luminosite;

    public String getAcuite() {
        return acuite;
    }

    public void setAcuite(String acuite) {
        this.acuite = acuite;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public int getScotome() {
        return scotome;
    }

    public void setScotome(int scotome) {
        this.scotome = scotome;
    }

    public boolean isScotome() {
        return isScotome;
    }

    public void setIsScotome(boolean isScotome) {
        this.isScotome = isScotome;
        if (isScotome) {
            setIsHemianopsie(false);
            setIsTubulaire(false);
        }
    }

    public int getTubulaire() {
        return tubulaire;
    }

    public void setTubulaire(int tubulaire) {
        this.tubulaire = tubulaire;
    }

    public boolean isTubulaire() {
        return isTubulaire;
    }

    public void setIsTubulaire(boolean isTubulaire) {
        this.isTubulaire = isTubulaire;
        if (isTubulaire) {
            setIsHemianopsie(false);
            setIsScotome(false);
        }
    }

    public int getHemianopsie() {
        return hemianopsie;
    }

    public void setHemianopsie(int hemianopsie) {
        this.hemianopsie = hemianopsie;
    }

    public boolean isHemianopsie() {
        return isHemianopsie;
    }

    public void setIsHemianopsie(boolean isHemianopsie) {
        this.isHemianopsie = isHemianopsie;
        if (isHemianopsie) {
            setIsTubulaire(false);
            setIsScotome(false);
        }
    }

    public int getContraste() {
        return contraste;
    }

    public void setContraste(int contraste) {
        this.contraste = contraste;
    }

    public int getLuminosite() {
        return luminosite;
    }

    public void setLuminosite(int luminosite) {
        this.luminosite = luminosite;
    }
}
