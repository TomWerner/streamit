import streamit.*;

public class PeekPipe extends StreamIt {
    public static void main(String args[]) {
        new PeekPipe().run();
    }
    public void init() {
        add(new Filter() {
                int x;
                Channel output = new Channel(Integer.TYPE, 1);
                public void initIO() {
                    this.streamOutput = output;
                }
                public void work() {
                    output.pushInt(x++);
                }
                public void init () { }
            });
        add(new Filter() {
                Channel input = new Channel(Integer.TYPE, 1, 10);
                public void initIO() {
                    this.streamInput = input;
                }
                public void work() {
                    int i;
                    int sum = 0;
                    for (i=0; i<10; i++) {
                        sum += input.peekInt(i);
                    }
                    System.out.println(sum);
                    input.popInt();
                }
                public void init () { }
            });
    }
}
