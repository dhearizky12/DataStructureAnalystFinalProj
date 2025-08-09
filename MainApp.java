import java.util.*;

/**
 * MainApp.java
 * A simple student management system using:
 *  - HashMap<String, Student> as Hash Table (fast lookup by NIM)
 *  - BST keyed by IPK (double) where each node stores a list of students with same IPK
 *
 * Features:
 *  - insert student
 *  - search by NIM (O(1) avg via HashMap)
 *  - search by IPK (via BST)
 *  - delete by NIM (removes from HashMap and BST)
 *  - inorder traversal of BST (students ascending by IPK)
 *
 * Compile: javac MainApp.java
 * Run    : java MainApp
 */
public class MainApp {
    // ---- Student model ----
    static class Student {
        String nim;
        String name;
        double ipk;

        public Student(String nim, String name, double ipk) {
            this.nim = nim;
            this.name = name;
            this.ipk = ipk;
        }

        @Override
        public String toString() {
            return String.format("NIM:%s | Name:%s | IPK:%.2f", nim, name, ipk);
        }
    }

    // ---- BST Node (key = ipk) ----
    static class BSTNode {
        double key; // ipk
        List<Student> students; // all students with this ipk
        BSTNode left, right;

        BSTNode(Student s) {
            this.key = s.ipk;
            this.students = new ArrayList<>();
            this.students.add(s);
        }
    }

    // ---- BST Manager ----
    static class BST {
        private BSTNode root;

        // insert student
        public void insert(Student s) {
            root = insertRec(root, s);
        }

        private BSTNode insertRec(BSTNode node, Student s) {
            if (node == null) {
                return new BSTNode(s);
            }
            if (Double.compare(s.ipk, node.key) == 0) {
                node.students.add(s);
            } else if (s.ipk < node.key) {
                node.left = insertRec(node.left, s);
            } else {
                node.right = insertRec(node.right, s);
            }
            return node;
        }

        // find students by exact IPK
        public List<Student> findByIpk(double ipk) {
            BSTNode node = findNode(root, ipk);
            return node == null ? Collections.emptyList() : new ArrayList<>(node.students);
        }

        private BSTNode findNode(BSTNode node, double ipk) {
            if (node == null) return null;
            if (Double.compare(ipk, node.key) == 0) return node;
            if (ipk < node.key) return findNode(node.left, ipk);
            return findNode(node.right, ipk);
        }

        // remove a student by NIM and its ipk; we assume caller provides ipk
        public void removeStudent(String nim, double ipk) {
            BSTNode node = findNode(root, ipk);
            if (node == null) return;
            // remove student in the list
            node.students.removeIf(s -> s.nim.equals(nim));
            // if no more students in this node, delete the node from BST
            if (node.students.isEmpty()) {
                root = deleteNode(root, ipk);
            }
        }

        // BST node deletion by key
        private BSTNode deleteNode(BSTNode node, double key) {
            if (node == null) return null;
            if (key < node.key) {
                node.left = deleteNode(node.left, key);
            } else if (key > node.key) {
                node.right = deleteNode(node.right, key);
            } else {
                // node to delete
                if (node.left == null) return node.right;
                if (node.right == null) return node.left;
                // two children: replace with inorder successor (min in right)
                BSTNode succ = minNode(node.right);
                node.key = succ.key;
                node.students = succ.students;
                node.right = deleteNode(node.right, succ.key);
            }
            return node;
        }

        private BSTNode minNode(BSTNode node) {
            while (node.left != null) node = node.left;
            return node;
        }

        // inorder traversal: returns ordered list of students (by ipk asc)
        public List<Student> inorder() {
            List<Student> list = new ArrayList<>();
            inorderRec(root, list);
            return list;
        }

        private void inorderRec(BSTNode node, List<Student> out) {
            if (node == null) return;
            inorderRec(node.left, out);
            // add students in the node (they share same ipk) - preserve insertion order
            out.addAll(node.students);
            inorderRec(node.right, out);
        }
    }

    // ---- StudentManager combining HashMap + BST ----
    static class StudentManager {
        private Map<String, Student> hashTable; // key: NIM
        private BST bst;

        public StudentManager() {
            this.hashTable = new HashMap<>();
            this.bst = new BST();
        }

        // insert new student; returns true if success, false if NIM exists
        public boolean insertStudent(String nim, String name, double ipk) {
            if (hashTable.containsKey(nim)) return false; // duplicate NIM not allowed
            Student s = new Student(nim, name, ipk);
            hashTable.put(nim, s);
            bst.insert(s);
            return true;
        }

        // search by NIM (fast)
        public Student searchByNim(String nim) {
            return hashTable.get(nim);
        }

        // search by IPK (may return many)
        public List<Student> searchByIpk(double ipk) {
            return bst.findByIpk(ipk);
        }

        // delete by NIM
        public boolean deleteByNim(String nim) {
            Student s = hashTable.remove(nim);
            if (s == null) return false;
            bst.removeStudent(nim, s.ipk);
            return true;
        }

        // list all students ordered by IPK ascending
        public List<Student> listAllOrderedByIpk() {
            return bst.inorder();
        }

        // stats
        public int totalStudents() {
            return hashTable.size();
        }
    }

    // ---- Simple CLI demo ----
    public static void main(String[] args) {
        StudentManager mgr = new StudentManager();

        // sample data (10-12 students)
        mgr.insertStudent("20231001", "Alice", 3.75);
        mgr.insertStudent("20231002", "Bob", 3.50);
        mgr.insertStudent("20231003", "Charlie", 3.75);
        mgr.insertStudent("20231004", "Dina", 3.90);
        mgr.insertStudent("20231005", "Eko", 3.20);
        mgr.insertStudent("20231006", "Fani", 3.50);
        mgr.insertStudent("20231007", "Gina", 3.10);
        mgr.insertStudent("20231008", "Hadi", 3.90);
        mgr.insertStudent("20231009", "Ika", 2.95);
        mgr.insertStudent("20231010", "Joko", 3.40);

        System.out.println("Total students inserted: " + mgr.totalStudents());
        System.out.println("\n--- Search by NIM ---");
        String nimToFind = "20231004";
        Student found = mgr.searchByNim(nimToFind);
        System.out.println("Search NIM " + nimToFind + ": " + (found == null ? "Not found" : found));

        System.out.println("\n--- Search by IPK (3.75) ---");
        List<Student> listIpk = mgr.searchByIpk(3.75);
        if (listIpk.isEmpty()) System.out.println("No student with IPK 3.75");
        else listIpk.forEach(s -> System.out.println(s));

        System.out.println("\n--- All students ordered by IPK (ascending) ---");
        List<Student> ordered = mgr.listAllOrderedByIpk();
        ordered.forEach(System.out::println);

        System.out.println("\n--- Delete student NIM 20231003 (Charlie) ---");
        boolean del = mgr.deleteByNim("20231003");
        System.out.println("Deleted? " + del);

        System.out.println("\n--- All students ordered by IPK after deletion ---");
        mgr.listAllOrderedByIpk().forEach(System.out::println);

        // interactive simple menu (optional)
        interactiveMenu(mgr);
    }

    private static void interactiveMenu(StudentManager mgr) {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\nMenu: 1-Add 2-SearchNIM 3-SearchIPK 4-DeleteNIM 5-ListAll 0-Exit");
            System.out.print("Choice> ");
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;
            int choice;
            try { choice = Integer.parseInt(line); } catch (NumberFormatException e) { continue; }
            switch (choice) {
                case 0:
                    System.out.println("Exit.");
                    sc.close();
                    return;
                case 1:
                    System.out.print("NIM: "); String nim = sc.nextLine().trim();
                    System.out.print("Name: "); String name = sc.nextLine().trim();
                    System.out.print("IPK: "); double ipk = Double.parseDouble(sc.nextLine().trim());
                    boolean ok = mgr.insertStudent(nim, name, ipk);
                    System.out.println(ok ? "Inserted." : "NIM already exists.");
                    break;
                case 2:
                    System.out.print("NIM: "); String q = sc.nextLine().trim();
                    Student s = mgr.searchByNim(q);
                    System.out.println(s == null ? "Not found." : s);
                    break;
                case 3:
                    System.out.print("IPK: "); double qipk = Double.parseDouble(sc.nextLine().trim());
                    List<Student> out = mgr.searchByIpk(qipk);
                    if (out.isEmpty()) System.out.println("No student with that IPK.");
                    else out.forEach(System.out::println);
                    break;
                case 4:
                    System.out.print("NIM: "); String d = sc.nextLine().trim();
                    boolean removed = mgr.deleteByNim(d);
                    System.out.println(removed ? "Deleted." : "NIM not found.");
                    break;
                case 5:
                    System.out.println("All students (by IPK asc):");
                    mgr.listAllOrderedByIpk().forEach(System.out::println);
                    break;
                default:
                    System.out.println("Unknown option.");
            }
        }
    }
}
