#!/bin/bash

Compile(){

	javac -d classes -cp lib/pddl4j-4.0.0.jar:lib/org.sat4j.core.jar src/fr/uga/pddl4j/yasp/*.java

}

Solve(){
	read -p "Enter domain file [path to the file]: " domainFile
	read -p "Enter problem file [path to the file]: " problemFile

    java -cp classes:lib/pddl4j-4.0.0.jar:lib/org.sat4j.core.jar -server -Xms2048m -Xmx2048m fr.uga.pddl4j.yasp.YetAnotherSATPlanner $domainFile $problemFile

}

show_menus() {
    echo "| 1. Compile SAT Planner"
    echo "| 2. Solve Domain/Problem"
    echo "| 3. Exit"
    echo " ----------"
}


read_options(){
    local choice
    read -p "Enter choice [1 - 3] : " choice
    case $choice in
        1) Compile ;;
        2) Solve ;;
        3) exit 0;;
        *) echo "Error..." && sleep 1 && return
    esac
}

# ----------------------------------------------
# Trap CTRL+C, CTRL+Z and quit singles
# ----------------------------------------------
trap '' SIGINT SIGQUIT SIGTSTP

# -----------------------------------
# Main logic - infinite loop
# ------------------------------------
echo "****************************************"
echo "Yet Another SAT Planner"
echo "****************************************"
echo ""
echo "Choose an option:"
echo ""
while true
do
	show_menus
	read_options
done
