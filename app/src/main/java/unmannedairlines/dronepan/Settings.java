package unmannedairlines.dronepan;

public class Settings {
    private int picturesPerRow;
    private int numberOfRow;

    public int getNumberOfRow() {
        return numberOfRow;
    }

    public int getPicturesPerRow() {
        return picturesPerRow;
    }

    public void setNumberOfRow(int numberOfRow) {
        this.numberOfRow = numberOfRow;
    }

    public void setPicturesPerRow(int picturesPerRow) {
        this.picturesPerRow = picturesPerRow;
    }
}
