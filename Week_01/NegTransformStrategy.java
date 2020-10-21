
public class NegTransformStrategy implements TransformStrategy{
    @Override
    public void transform(byte[] in, int length){
        byte ORG = (byte) 255;
        for (int i = 0; i < length; i++) {
            in[i] = (byte) (ORG - in[i]);
        }
    }
}
