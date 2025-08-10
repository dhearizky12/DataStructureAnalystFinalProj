import java.util.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;


/**
 * MainApp.java
 * A simple student management system using:
 * - HashMap<String, Student> as Hash Table (fast lookup by NIM)
 * - BST keyed by IPK (double) where each node stores a list of students with same IPK
 *
 * Features:
 * - insert student
 * - search by NIM (O(1) avg via HashMap)
 * - search by IPK (via BST)
 * - delete by NIM (removes from HashMap and BST)
 * - inorder traversal of BST (students ascending by IPK)
 * - Batch upload from .txt file
 *
 * Compile: javac MainApp.java
 * Run    : java MainApp
 */
public class MainApp 
{
    // ---- Student model ----
    static class Student 
    {
        String nim;
        String name;
        String major;
        double ipk;
        List<String> courses;

        public Student(String nim, String name, String major, double ipk) 
        {
            this.nim = nim;
            this.name = name;
            this.major = major;
            this.ipk = ipk;
            this.courses = new ArrayList<>();
        }

        public void addCourses(String course)
        {
            if ( !courses.contains(course))
            {
                courses.add(course);
            }
        }

        @Override
        public String toString() 
        {
            return String.format("NIM:%s | Name:%s | | Jurusan:%s | IPK:%.2f | MK:%s ", nim, 
            name, major, ipk, courses.isEmpty()? "Belum ada" : String.join(", ", courses));
        }
    }

    // ---- BST Node (key = ipk) ----
    static class BSTNode 
    {
        double key; // ipk
        List<Student> students; // all students with this ipk
        BSTNode left, right;

        BSTNode(Student studentEntry) 
        {
            this.key = studentEntry.ipk;
            this.students = new ArrayList<>();
            this.students.add(studentEntry);
        }
    }

    // ---- BST Manager ----
    static class BST 
    {
        private BSTNode root;

        // insert student
        public void insert(Student studentEntry) 
        {
            root = insertRec(root, studentEntry);
        }

        private BSTNode insertRec(BSTNode node, Student studentEntry) 
        {
            if (node == null) {
                return new BSTNode(studentEntry);
            }
            if (Double.compare(studentEntry.ipk, node.key) == 0) {
                node.students.add(studentEntry);
            } else if (studentEntry.ipk < node.key) {
                node.left = insertRec(node.left, studentEntry);
            } else {
                node.right = insertRec(node.right, studentEntry);
            }
            return node;
        }

        // find students by exact IPK
        public List<Student> findByIpk(double ipk) 
        {
            BSTNode node = findNode(root, ipk);
            return node == null ? Collections.emptyList() : new ArrayList<>(node.students);
        }

        private BSTNode findNode(BSTNode node, double ipk) 
        {
            if (node == null) return null;
            if (Double.compare(ipk, node.key) == 0) return node;
            if (ipk < node.key) return findNode(node.left, ipk);
            return findNode(node.right, ipk);
        }

        // remove a student by NIM and its ipk; we assume caller provides ipk
        public void removeStudent(String nim, double ipk) 
        {
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
                BSTNode successorNode = minNode(node.right);
                node.key = successorNode.key;
                node.students = successorNode.students;
                node.right = deleteNode(node.right, successorNode.key);
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

    
// ------------------- Graph for Majors -------------------
    static class Graph 
    {
        private Map<String, List<String>> adj = new HashMap<>();

        public void addNode(String node) 
        {
            adj.putIfAbsent(node, new ArrayList<>());
        }

        public void addEdge(String from, String to) 
        {
            addNode(from);
            addNode(to);
            adj.get(from).add(to);
            adj.get(to).add(from); // undirected
        }

        public void printGraph() 
        {
            for (String node : adj.keySet()) {
                System.out.println(node + " -> " + adj.get(node));
            }
        }

        public List<String> bfs(String start) 
        {
            List<String> visited = new ArrayList<>();
            if (!adj.containsKey(start)) return visited;
            Queue<String> q = new LinkedList<>();
            Set<String> seen = new HashSet<>();
            q.add(start);
            seen.add(start);
            while (!q.isEmpty()) {
                String cur = q.poll();
                visited.add(cur);
                for (String neigh : adj.get(cur)) 
                {
                    if (!seen.contains(neigh)) {
                        seen.add(neigh);
                        q.add(neigh);
                    }
                }
            }
            return visited;
        }
    }

    // ---- StudentManager combining HashMap + BST ----
    static class StudentManager {
        private Map<String, Student> hashTable; // key: NIM
        private BST bst;

        public StudentManager() 
        {
            this.hashTable = new HashMap<>();
            this.bst = new BST();
        }

        // insert new student; returns true if success, false if NIM exists
        public boolean insertStudent(String nim, String name, String major, double ipk) {
            if (hashTable.containsKey(nim)) return false; // duplicate NIM not allowed
            Student studentEntry = new Student(nim, name, major, ipk);
            hashTable.put(nim, studentEntry);
            bst.insert(studentEntry);
            return true;
        }

        /**
         * [NEW METHOD] upload batch menggunakan .txt file.
         * format per line: NIM,Name,IPK
         */
        public void batchUploadFromFile(String filePath) {
            try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
                stream.forEach(line -> {
                    String[] parts = line.split(",");
                    if (parts.length != 3) {
                        System.out.println(">> SKIPPED: Invalid format -> " + line);
                        return;
                    }
                    try {
                        String nim = parts[0].trim();
                        String name = parts[1].trim();
                        String major = parts[2].trim();
                        double ipk = Double.parseDouble(parts[2].trim());
                        if (!insertStudent(nim, name, major, ipk)) {
                            System.out.println(">> SKIPPED: NIM already exists -> " + nim);
                        }
                    } catch (NumberFormatException e) {
                        System.out.println(">> SKIPPED: Invalid IPK format -> " + line);
                    }
                });
                System.out.println("\n>> Batch upload process finished.");
                System.out.println(">> Total students now: " + totalStudents());

            } catch (IOException e) {
                System.out.println(">> ERROR: File not found or cannot be read -> " + filePath);
            }
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

        public boolean addCourseToStudent(String nim, String course) 
        {
            Student studentEntry = hashTable.get(nim);
            if (studentEntry == null) return false;
            studentEntry.addCourses(course);
            return true;
        }
    }

    // ---- Utility: print elapsed nicely ----
    private static void printElapsed(String label, long nanos) {
        System.out.printf(">> %s - Waktu Eksekusi (Diluar dari Input): %.3f ms (%d ns)%n", label, nanos / 1_000_000.0, nanos);
    }

    // ---- Simple CLI demo ----
    public static void main(String[] args) {
        // Create a new student manager instance
        StudentManager mgr = new StudentManager();
        Graph majorGraph = new Graph();

        // Sample majors graph
        majorGraph.addEdge("Informatika", "Sistem Informasi");
        majorGraph.addEdge("Informatika", "Teknik Elektro");
        majorGraph.addEdge("Sistem Informasi", "Manajemen");
        majorGraph.addEdge("Teknik Elektro", "Fisika");

        // Welcome message
        System.out.println("==============================================");
        System.out.println("Sistem Manajemen Mahasiswa");
        System.out.println("Database saat ini kosong.");
        System.out.println("==============================================");

        // Directly start the interactive menu for user input
        interactiveMenu(mgr, majorGraph);
    }

    private static void interactiveMenu(StudentManager mgr, Graph majorGraph) 
    {
        Scanner sc = new Scanner(System.in);
        while (true) {

            System.out.println("\n=== Sistem Manajemen Mahasiswa ===");
            System.out.println("1. Tambah Mahasiswa");
            System.out.println("2. Cari Mahasiswa by NIM");
            System.out.println("3. Cari Mahasiswa by IPK");
            System.out.println("4. Hapus Mahasiswa by NIM");
            System.out.println("5. List Semua Mahasiswa (IPK Asc)");
            System.out.println("6. Impor Data Mahasiswa dari .txt File");
            System.out.println("7. Tambah Mata Kuliah ke Mahasiswa");
            System.out.println("8. Lihat Graph Jurusan");
            System.out.println("9. BFS Graph Jurusan");
            System.out.println("0. Exit");
            System.out.print("Pilih: ");
            System.out.print("Choice> ");
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;
            int choice;
            try {
                choice = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
                continue;
            }

            switch (choice) {
                case 0:
                    System.out.println("Exit.");
                    sc.close();
                    return;

                case 1: {
                    // Add student (time only the processing, not input)
                    System.out.print("NIM: "); String nim = sc.nextLine().trim();
                    System.out.print("Name: "); String name = sc.nextLine().trim();
                    System.out.print("Jurusan: "); String major = sc.nextLine();
                    System.out.print("IPK: ");
                    double ipk;
                    try {
                        ipk = Double.parseDouble(sc.nextLine().trim());
                    } catch (NumberFormatException e) {
                        System.out.println(">> ERROR: Invalid IPK format. Please use a number (e.g., 3.75).");
                        break;
                    }
                    long t0 = System.nanoTime();
                    boolean ok = mgr.insertStudent(nim, name, major, ipk);
                    long t1 = System.nanoTime();
                    System.out.println(ok ? ">> Inserted successfully." : ">> ERROR: NIM already exists.");
                    printElapsed("Insert", t1 - t0);
                    break;
                }

                case 2: {
                    // Search by NIM
                    System.out.print("NIM to search: "); String q = sc.nextLine().trim();
                    long t0 = System.nanoTime();
                    Student s = mgr.searchByNim(q);
                    long t1 = System.nanoTime();
                    System.out.println(s == null ? ">> Not found." : ">> Found: " + s);
                    printElapsed("Search by NIM", t1 - t0);
                    break;
                }

                case 3: {
                    // Search by IPK
                    System.out.print("IPK to search: ");
                    double ipkNeedToSearch;
                    try {
                        ipkNeedToSearch = Double.parseDouble(sc.nextLine().trim());
                    } catch (NumberFormatException e) {
                        System.out.println(">> ERROR: Invalid IPK format. Please use a number (e.g., 3.75).");
                        break;
                    }
                    long t0 = System.nanoTime();
                    List<Student> outputUser = mgr.searchByIpk(ipkNeedToSearch);
                    long t1 = System.nanoTime();
                    if (outputUser.isEmpty()) {
                        System.out.println(">> No student found with that IPK.");
                    } else {
                        System.out.println(">> Found " + outputUser.size() + " student(s):");
                        outputUser.forEach(System.out::println);
                    }
                    printElapsed("Search by IPK", t1 - t0);
                    break;
                }

                case 4: {
                    // Delete by NIM
                    System.out.print("NIM to delete: "); String d = sc.nextLine().trim();
                    long t0 = System.nanoTime();
                    boolean removed = mgr.deleteByNim(d);
                    long t1 = System.nanoTime();
                    System.out.println(removed ? ">> Deleted successfully." : ">> ERROR: NIM not found.");
                    printElapsed("Delete by NIM", t1 - t0);
                    break;
                }

                case 5: {
                    // List all (measure traversal only, not printing)
                    System.out.println("--- All students (ordered by IPK ascending) ---");
                    long t0 = System.nanoTime();
                    List<Student> allStudents = mgr.listAllOrderedByIpk();
                    long t1 = System.nanoTime();
                    if (allStudents.isEmpty()) {
                        System.out.println(">> Database is empty.");
                    } else {
                        allStudents.forEach(System.out::println);
                    }
                    printElapsed("List (BST inorder traversal)", t1 - t0);
                    break;
                }

                case 6: {
                    // Batch upload (measure file read + processing)
                    System.out.print("Enter .txt file path (e.g., data.txt): ");
                    String filePath = sc.nextLine().trim();
                    long t0 = System.nanoTime();
                    mgr.batchUploadFromFile(filePath);
                    long t1 = System.nanoTime();
                    printElapsed("Batch upload", t1 - t0);
                    break;
                }

                case 7: {
                    // input mata kuliah to student data
                    System.out.print("NIM: ");
                    String nimC = sc.nextLine();
                    System.out.print("Nama Mata Kuliah: ");
                    String course = sc.nextLine();
                    System.out.println(mgr.addCourseToStudent(nimC, course)
                            ? "Mata kuliah ditambahkan."
                            : "Mahasiswa tidak ditemukan.");
                }

                case 8: {
                    majorGraph.printGraph();
                    break;
                }

                case 9 : {
                    System.out.print("Mulai BFS dari jurusan: ");
                    String start = sc.nextLine();
                    List<String> bfs = majorGraph.bfs(start);
                    if (bfs.isEmpty()) System.out.println("Jurusan tidak ditemukan di graph.");
                    else System.out.println("Hasil BFS: " + String.join(" -> ", bfs));
                    break;
                }

                default:
                    System.out.println(">> Unknown option.");
            }
        }
    }
}
