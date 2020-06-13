#include <iostream>
#include <fstream>
#include <unistd.h>
#include <fcntl.h>
#include <stdio.h>
#include <linux/input.h>
 
using namespace std;
 
int main(int argc, char *argv []) {
    int fd;
    struct input_event ev;
    ofstream file;
    file.open("output_file.txt");
 
    fd = open("/dev/input/event0", O_RDONLY);
 
    if (fd < 0) {
        perror("annot open file");
        return 1;
    }
int i = 0;
    while (1) {
        if (read(fd, &ev, sizeof(struct input_event)) < 0) {
            perror("Cannot read");
            return 1;
        }
 
        if (ev.type == EV_KEY && i % 2 == 0) {
 
            printf("Event type: %d\n"
            "\tcode: %d\n"
            "\tValue: %d\n", ev.type, ev.code, ev.value);
            file << ev.code << endl;
        }
 
        i++;
    }
 
    file.close();
    return 0;
}